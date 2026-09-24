package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.database.daos.AppPreferenceDao
import com.example.data.local.database.daos.HistoryDao
import com.example.data.local.database.daos.RecentInputDao
import com.example.data.local.database.daos.SettingsDao
import com.example.data.local.database.entities.AppPreferenceEntity
import com.example.data.local.database.entities.HistoryEntity
import com.example.data.local.database.entities.RecentInputEntity
import com.example.data.local.database.entities.SettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        HistoryEntity::class,
        SettingsEntity::class,
        RecentInputEntity::class,
        AppPreferenceEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun recentInputDao(): RecentInputDao
    abstract fun appPreferenceDao(): AppPreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "verilens_ai.db"
                ).fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial verification data so the app has realistic content
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).historyDao().insertAll(getInitialDemoHistory())
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        fun getInitialDemoHistory(): List<HistoryEntity> {
            val now = System.currentTimeMillis()
            return listOf(
                HistoryEntity(
                    id = 1,
                    title = "Viral Health Claim: Miracle Cure Tea",
                    snippet = "Claim circulating that drinking cold ginger-lemon infusion cures viral respiratory infection within 6 hours.",
                    inputType = "SCREENSHOT",
                    credibilityScore = 22,
                    verdict = "MISLEADING",
                    summary = "Multiple health authorities confirm that while ginger and lemon support general hydration, there is zero scientific basis for a rapid viral cure.",
                    sourcesCount = 3,
                    timestamp = now - 3600000 * 2,
                    isBookmarked = true,
                    originalClaim = "Drinking cold ginger-lemon infusion cures viral respiratory infection within 6 hours.",
                    verifiedInformation = "No clinical trial supports rapid viral clearance via herbal infusions. Standard clinical treatments rely on antiviral therapies and symptom management.",
                    reasoning = "World Health Organization mythbusters and PubMed clinical reviews consistently show herbal infusions do not cure viral infections. The claim misinterprets general dietary hydration benefits.",
                    category = "HEALTH",
                    evidenceSummaryJson = "[\"WHO Clarification: No cure exists in the form of dietary herbal infusions.\",\"PubMed Clinical Review: In-vitro herbal observations do not translate to in-vivo viral eradication.\",\"HealthFeedback Medical Fact Check: Attributed medical trial was fabricated.\"]",
                    sourcesJson = "[{\"name\":\"WHO Mythbusters\",\"publisher\":\"World Health Organization\",\"title\":\"Coronavirus & Respiratory Disease Misconceptions\",\"date\":\"Recent\",\"category\":\"HEALTH\",\"trustLevel\":\"High\",\"citationUrl\":\"https://www.who.int/emergencies/diseases/novel-coronavirus-2019/advice-for-public/myth-busters\"},{\"name\":\"PubMed Central\",\"publisher\":\"National Institutes of Health\",\"title\":\"Herbal Infusions and Clinical Viral Immunology\",\"date\":\"2023\",\"category\":\"HEALTH\",\"trustLevel\":\"High\",\"citationUrl\":\"https://pubmed.ncbi.nlm.nih.gov\"},{\"name\":\"Health Feedback\",\"publisher\":\"Science Feedback Network\",\"title\":\"Viral Tea Claims Lack Biological Plausibility\",\"date\":\"Recent\",\"category\":\"HEALTH\",\"trustLevel\":\"High\",\"citationUrl\":\"https://healthfeedback.org\"}]",
                    recommendationsJson = "[\"Do not stop prescribed medical treatments for herbal drinks\",\"Check WHO or CDC official guidance for respiratory infection therapies\",\"Do not forward unverified health tips in group chats\"]",
                    disclaimer = "VeriLens AI cross-references information with reputable public databases. Always consult licensed medical providers for health decisions."
                ),
                HistoryEntity(
                    id = 2,
                    title = "Official Public Transit Fare Update",
                    snippet = "Municipal transportation authority announcement regarding student pass discounts starting next semester.",
                    inputType = "SCREENSHOT",
                    credibilityScore = 95,
                    verdict = "VERIFIED",
                    summary = "Matches official press releases published on the Department of Transportation verified portal and local city news feeds.",
                    sourcesCount = 2,
                    timestamp = now - 3600000 * 18,
                    isBookmarked = false,
                    originalClaim = "Municipal transportation authority announced 50% discount for registered students starting next semester.",
                    verifiedInformation = "The transport council approved subsidized semester transit passes for registered undergraduate and postgraduate students.",
                    reasoning = "Official press circular was published on the Department of Transportation website and verified across metropolitan council minutes.",
                    category = "GOVERNMENT",
                    evidenceSummaryJson = "[\"Department of Transportation gazette circular published on official portal.\",\"Metropolitan Council meeting minutes confirm budget allocation for student discount passes.\"]",
                    sourcesJson = "[{\"name\":\"Department of Transportation\",\"publisher\":\"Gov Gazette\",\"title\":\"Student Concession Pass Scheme Notice\",\"date\":\"Recent\",\"category\":\"GOVERNMENT\",\"trustLevel\":\"High\",\"citationUrl\":\"https://www.transport.gov\"},{\"name\":\"City Council News Wire\",\"publisher\":\"City Press Bureau\",\"title\":\"Metropolitan Transit Budget Approves Student Subsidy\",\"date\":\"Recent\",\"category\":\"GOVERNMENT\",\"trustLevel\":\"High\",\"citationUrl\":\"https://www.metrotransit.org\"}]",
                    recommendationsJson = "[\"Eligible students can apply through the official transit portal\",\"Present institutional ID when claiming subsidy\",\"Refer to the official transport link for application deadlines\"]",
                    disclaimer = "VeriLens AI cross-references information with reputable public databases."
                )
            )
        }
    }
}
