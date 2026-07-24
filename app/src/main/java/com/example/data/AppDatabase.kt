package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        SchoolEntity::class,
        RentalEntity::class,
        SuggestionEntity::class,
        RosterNameEntity::class,
        UniformCheckEntity::class,
        AttendanceEntity::class,
        MeritLogEntity::class,
        CleanZoneEntity::class,
        FundEntity::class,
        VoteEntity::class,
        LostItemEntity::class,
        ItemStockEntity::class,
        InviteCodeEntity::class,
        WrongAnswerEntity::class,
        QuestionEntity::class,
        ProjectGroupEntity::class,
        ProjectTaskEntity::class,
        ProjectResourceEntity::class,
        ProjectEvaluationEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "integrated_management_db"
                )
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
