package com.example.webrtc

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule

class WebRtcManager(private val context: Context) {
    companion object {
        private const val TAG = "WebRtcManager"
        const val PERMISSION_REQUEST_CODE = 1001
    }

    val eglBase: EglBase = EglBase.create()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var audioDeviceModule: AudioDeviceModule? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null

    var remoteVideoTrack: VideoTrack? = null
        private set

    private var currentCallId: String? = null
    private var isCaller: Boolean = false
    private var firebaseDb: FirebaseDatabase? = null

    var onRemoteTrackReceived: ((VideoTrack) -> Unit)? = null

    init {
        initWebRtc()
    }

    fun checkAndRequestAudioPermission(activity: Activity): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun initWebRtc() {
        try {
            val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initializationOptions)

            // Create JavaAudioDeviceModule for WebRTC audio with hardware echo cancellation and noise suppression
            val adm = JavaAudioDeviceModule.builder(context)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule()
            audioDeviceModule = adm

            val options = PeerConnectionFactory.Options()
            val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory()

            Log.d(TAG, "WebRTC PeerConnectionFactory initialized successfully with JavaAudioDeviceModule.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing WebRTC", e)
        }
    }

    fun startCallAudio() {
        Handler(Looper.getMainLooper()).post {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = true
                am.isMicrophoneMute = false
            }
            Log.d(TAG, "WebRTC Call Audio Started: MODE_IN_COMMUNICATION")
        }
    }

    fun stopCallAudio() {
        Handler(Looper.getMainLooper()).post {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_NORMAL
                am.isSpeakerphoneOn = false
            }
            Log.d(TAG, "WebRTC Call Audio Stopped: MODE_NORMAL")
        }
    }

    fun startLocalAudioAndVideo(isVideo: Boolean = true, localRenderer: SurfaceViewRenderer? = null) {
        val factory = peerConnectionFactory ?: return

        // Audio track with noise suppression & echo cancellation constraints
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }
        val audioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource)
        localAudioTrack?.setEnabled(true)

        // Video
        if (isVideo) {
            videoCapturer = createCameraCapturer()
            if (videoCapturer != null) {
                val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
                val videoSource = factory.createVideoSource(videoCapturer!!.isScreencast)
                videoCapturer!!.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
                videoCapturer!!.startCapture(640, 480, 30)

                localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource)
                localVideoTrack?.setEnabled(true)

                if (localRenderer != null) {
                    localRenderer.init(eglBase.eglBaseContext, null)
                    localRenderer.setMirror(true)
                    localRenderer.setEnableHardwareScaler(true)
                    localVideoTrack?.addSink(localRenderer)
                }
            }
        }
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    fun initPeerConnection(
        callId: String,
        isCaller: Boolean,
        db: FirebaseDatabase,
        remoteRenderer: SurfaceViewRenderer? = null
    ) {
        this.currentCallId = callId
        this.isCaller = isCaller
        this.firebaseDb = db

        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        )
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE Connection State: $state")
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    startCallAudio()
                } else if (state == PeerConnection.IceConnectionState.DISCONNECTED ||
                    state == PeerConnection.IceConnectionState.CLOSED ||
                    state == PeerConnection.IceConnectionState.FAILED
                ) {
                    stopCallAudio()
                }
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate == null) return
                val candMap = mapOf(
                    "sdpMid" to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex,
                    "candidate" to candidate.sdp
                )
                val targetPath = if (isCaller) "candidates/caller" else "candidates/callee"
                db.getReference("calls").child(callId).child(targetPath).push().setValue(candMap)
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                if (stream != null && stream.videoTracks.isNotEmpty()) {
                    val track = stream.videoTracks[0]
                    remoteVideoTrack = track
                    if (remoteRenderer != null) {
                        remoteRenderer.init(eglBase.eglBaseContext, null)
                        remoteRenderer.setEnableHardwareScaler(true)
                        track.addSink(remoteRenderer)
                    }
                    onRemoteTrackReceived?.invoke(track)
                }
            }

            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is AudioTrack) {
                    track.setEnabled(true)
                    track.setVolume(1.0)
                }
                if (track is VideoTrack) {
                    remoteVideoTrack = track
                    if (remoteRenderer != null) {
                        remoteRenderer.init(eglBase.eglBaseContext, null)
                        remoteRenderer.setEnableHardwareScaler(true)
                        track.addSink(remoteRenderer)
                    }
                    onRemoteTrackReceived?.invoke(track)
                }
            }
        })

        // Add Local Audio Track with SEND_RECV transceiver direction
        localAudioTrack?.let { audioTrack ->
            val init = RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_RECV)
            peerConnection?.addTransceiver(audioTrack, init)
        }

        // Add Local Video Track if present
        val mediaStreamLabels = listOf("ARDAMS")
        localVideoTrack?.let { peerConnection?.addTrack(it, mediaStreamLabels) }

        // Observe ICE candidates
        observeIceCandidates(callId, !isCaller, db)

        if (isCaller) {
            createOffer(callId, db)
        } else {
            observeOfferAndCreateAnswer(callId, db)
        }
    }

    private fun createOffer(callId: String, db: FirebaseDatabase) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) return
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)

                val sdpMap = mapOf(
                    "type" to sdp.type.canonicalForm(),
                    "sdp" to sdp.description
                )
                db.getReference("calls").child(callId).child("sdp/offer").setValue(sdpMap)
            }
        }, constraints)

        // Observe Answer
        db.getReference("calls").child(callId).child("sdp/answer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val typeStr = snapshot.child("type").value as? String ?: return
                        val sdpStr = snapshot.child("sdp").value as? String ?: return
                        val sdp = SessionDescription(SessionDescription.Type.fromCanonicalForm(typeStr), sdpStr)
                        peerConnection?.setRemoteDescription(SdpObserverAdapter(), sdp)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun observeOfferAndCreateAnswer(callId: String, db: FirebaseDatabase) {
        db.getReference("calls").child(callId).child("sdp/offer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val typeStr = snapshot.child("type").value as? String ?: return
                        val sdpStr = snapshot.child("sdp").value as? String ?: return
                        val sdp = SessionDescription(SessionDescription.Type.fromCanonicalForm(typeStr), sdpStr)

                        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
                            override fun onSetSuccess() {
                                createAnswer(callId, db)
                            }
                        }, sdp)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun createAnswer(callId: String, db: FirebaseDatabase) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) return
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)

                val sdpMap = mapOf(
                    "type" to sdp.type.canonicalForm(),
                    "sdp" to sdp.description
                )
                db.getReference("calls").child(callId).child("sdp/answer").setValue(sdpMap)
            }
        }, constraints)
    }

    private fun observeIceCandidates(callId: String, observeFromPathIsCaller: Boolean, db: FirebaseDatabase) {
        val path = if (observeFromPathIsCaller) "candidates/caller" else "candidates/callee"
        db.getReference("calls").child(callId).child(path)
            .addChildEventListener(object : com.google.firebase.database.ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val sdpMid = snapshot.child("sdpMid").value as? String ?: return
                    val sdpMLineIndex = (snapshot.child("sdpMLineIndex").value as? Long)?.toInt() ?: 0
                    val sdp = snapshot.child("candidate").value as? String ?: return

                    val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                    peerConnection?.addIceCandidate(candidate)
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun setMute(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun setSpeakerphoneOn(on: Boolean) {
        audioManager?.isSpeakerphoneOn = on
    }

    fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun close() {
        try {
            stopCallAudio()
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            localVideoTrack?.dispose()
            localAudioTrack?.dispose()
            peerConnection?.close()
            peerConnection = null

            audioDeviceModule?.release()
            audioDeviceModule = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing WebRTC", e)
        }
    }
}

open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}

