package dev.amenokizele.tervyn.navigation

sealed class TervynDestination(val route: String) {
    data object Bootstrap : TervynDestination("bootstrap")
    data object Login : TervynDestination("login")
    data object Jobs : TervynDestination("jobs")
    data object Sync : TervynDestination("sync")
    data object Settings : TervynDestination("settings")
    data object Logout : TervynDestination("logout")

    data object JobDetail : TervynDestination("job_detail/{jobId}") {
        fun createRoute(jobId: String): String = "job_detail/$jobId"
    }

    data object Execution : TervynDestination("execution/{jobId}") {
        fun createRoute(jobId: String): String = "execution/$jobId"
    }

    data object AddNote : TervynDestination("add_note/{jobId}") {
        fun createRoute(jobId: String): String = "add_note/$jobId"
    }

    data object AddPhoto : TervynDestination("add_photo/{jobId}") {
        fun createRoute(jobId: String): String = "add_photo/$jobId"
    }

    data object PhotoViewer : TervynDestination("photo_viewer/{jobId}/{photoId}") {
        fun createRoute(jobId: String, photoId: String): String = "photo_viewer/$jobId/$photoId"
    }

    data object CompleteJob : TervynDestination("complete_job/{jobId}") {
        fun createRoute(jobId: String): String = "complete_job/$jobId"
    }
}
