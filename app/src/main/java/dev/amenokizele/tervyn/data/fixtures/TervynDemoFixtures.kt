package dev.amenokizele.tervyn.data.fixtures

import dev.amenokizele.tervyn.domain.model.Attachment
import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.ChecklistItem
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.Note
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.model.User
import java.time.Instant

object TervynDemoFixtures {
    const val CURRENT_USER_ID = "user-amina"
    const val SUPPORT_USER_ID = "support-n1"

    private val day = "2026-09-03"

    val currentUser = User(
        id = CURRENT_USER_ID,
        email = "amina@tervyn.demo",
        firstName = "Amina",
        lastName = "Kabwe",
        jobTitle = "Technicienne terrain",
        avatarUrl = null,
        createdAt = instant("2026-09-01T08:00:00Z"),
        updatedAt = instant("2026-09-03T06:00:00Z"),
        lastSyncedAt = instant("2026-09-03T06:00:00Z")
    )

    val initialLastSyncAt: Instant = instant("${day}T06:00:00Z")

    fun initialJobs(): List<Job> = listOf(
        job(
            id = "job-001",
            reference = "JOB-2026-00182",
            title = "Installation routeur",
            description = "Déploiement d'un nouveau routeur d'entreprise et raccordement au switch principal de la baie de brassage.",
            clientName = "Alpha Distribution",
            siteName = "Kolwezi Centre",
            siteAddress = "14 Avenue des Usines, Quartier Industriel, Kolwezi",
            priority = JobPriority.HIGH,
            status = JobStatus.ASSIGNED,
            scheduledAt = at("08:30"),
            checklist = checklist("job-001", listOf(
                item("c1-1", "Vérifier l'alimentation", 1, true, false),
                item("c1-2", "Inspecter le câblage", 2, true, false),
                item("c1-3", "Configurer le routeur", 3, true, false),
                item("c1-4", "Tester la connexion", 4, false, false)
            )),
            notes = listOf(note("n1-1", "job-001", SUPPORT_USER_ID, "Prévoir un câble RJ45 Cat6 de 5 mètres supplémentaire.", at("07:45"))),
            attachments = emptyList()
        ),
        job(
            id = "job-002",
            reference = "JOB-2026-00183",
            title = "Inspection groupe électrogène",
            description = "Contrôle préventif trimestriel, vérification des niveaux d'huile, filtres et test de démarrage autonome.",
            clientName = "Horizon Depot",
            siteName = "Hangar Logistique B",
            siteAddress = "Route Nationale 39, Km 12, Kolwezi",
            priority = JobPriority.NORMAL,
            status = JobStatus.ASSIGNED,
            scheduledAt = at("09:45"),
            checklist = checklist("job-002", listOf(
                item("c2-1", "Vérifier niveau de liquide de refroidissement", 1, true, false),
                item("c2-2", "Contrôler batterie de démarrage", 2, true, false),
                item("c2-3", "Essai de basculement source normale/secours", 3, true, false),
                item("c2-4", "Nettoyage filtre à air", 4, false, false)
            ))
        ),
        job(
            id = "job-003",
            reference = "JOB-2026-00184",
            title = "Maintenance climatisation",
            description = "Nettoyage des échangeurs thermiques et recharge fluide frigorigène salle serveurs.",
            clientName = "Kivu Services",
            siteName = "Datacenter Est",
            siteAddress = "88 Boulevard de la Libération, Lubumbashi",
            priority = JobPriority.HIGH,
            status = JobStatus.IN_PROGRESS,
            scheduledAt = at("11:00"),
            startedAt = at("11:05"),
            syncState = SyncState.PENDING,
            checklist = checklist("job-003", listOf(
                item("c3-1", "Relevé températures entrée / sortie", 1, true, true),
                item("c3-2", "Inspection de l'évacuation des condensats", 2, true, true),
                item("c3-3", "Remplacement des filtres anti-poussière", 3, true, false),
                item("c3-4", "Test des sondes d'hygrométrie", 4, false, false)
            )),
            notes = listOf(note("n3-1", "job-003", CURRENT_USER_ID, "Filtre primaire fortement encrassé par les vents de sable.", at("11:15"), SyncState.PENDING)),
            attachments = listOf(attachment("p3-1", "job-003", "Filtre salle serveurs", "FILTER", at("11:18"), SyncState.PENDING))
        ),
        job(
            id = "job-004",
            reference = "JOB-2026-00185",
            title = "Audit câblage réseau",
            description = "Recette et certification réflectométrie optique sur la dorsale inter-bâtiments.",
            clientName = "Lualaba Office",
            siteName = "Bâtiment Administratif",
            siteAddress = "Avenue Kasavubu, Immeuble Horizon, Kolwezi",
            priority = JobPriority.NORMAL,
            status = JobStatus.ASSIGNED,
            scheduledAt = at("13:30"),
            checklist = checklist("job-004", listOf(
                item("c4-1", "Repérage des tiroirs optiques", 1, true, false),
                item("c4-2", "Test d'atténuation sur 12 brins", 2, true, false),
                item("c4-3", "Étiquetage des jarretières", 3, false, false)
            ))
        ),
        job(
            id = "job-005",
            reference = "JOB-2026-00186",
            title = "Remplacement onduleur",
            description = "Remplacement d'urgence de l'onduleur 3kVA en défaut batterie sur le standard téléphonique.",
            clientName = "Mamba Logistics",
            siteName = "Quai de chargement",
            siteAddress = "Zone Portuaire Fluviale, Bâtiment 4",
            priority = JobPriority.URGENT,
            status = JobStatus.ASSIGNED,
            scheduledAt = at("14:15"),
            checklist = checklist("job-005", listOf(
                item("c5-1", "Consignation électrique du coffret", 1, true, false),
                item("c5-2", "Dépose de l'ancien onduleur HS", 2, true, false),
                item("c5-3", "Pose et raccordement du nouvel onduleur", 3, true, false),
                item("c5-4", "Test de décharge sur charge résistive", 4, true, false)
            ))
        ),
        job(
            id = "job-006",
            reference = "JOB-2026-00187",
            title = "Mise en service imprimante",
            description = "Installation d'un copieur multifonction réseau et intégration à l'annuaire d'entreprise.",
            clientName = "Nova Trading",
            siteName = "Agence Centrale",
            siteAddress = "25 Avenue Laurent Kabila, Kolwezi",
            priority = JobPriority.LOW,
            status = JobStatus.ASSIGNED,
            scheduledAt = at("15:30"),
            checklist = checklist("job-006", listOf(
                item("c6-1", "Déballage et fixation des bacs papier", 1, true, false),
                item("c6-2", "Attribution IP statique et VLAN bureautique", 2, true, false),
                item("c6-3", "Impression de page de test calibrée", 3, true, false)
            ))
        ),
        job(
            id = "job-007",
            reference = "JOB-2026-00188",
            title = "Dépannage borne Wi-Fi",
            description = "Perte de signal récurrente dans l'atelier mécanique, vérifier PoE et switch d'étage.",
            clientName = "Gecamines Mines",
            siteName = "Atelier Mécanique Sud",
            siteAddress = "Route d'Uvira, Site Minier 2",
            priority = JobPriority.HIGH,
            status = JobStatus.COMPLETED,
            scheduledAt = at("07:00"),
            startedAt = at("07:05"),
            completedAt = at("07:55"),
            checklist = checklist("job-007", listOf(
                item("c7-1", "Contrôle tension injecteur PoE", 1, true, true),
                item("c7-2", "Test câble RJ45 blindé", 2, true, true),
                item("c7-3", "Mise à jour firmware AP", 3, true, true)
            )),
            notes = listOf(note("n7-1", "job-007", CURRENT_USER_ID, "Connecteur RJ45 oxydé par l'humidité, re-serti à neuf.", at("07:40"))),
            attachments = listOf(attachment("p7-1", "job-007", "Connecteur remplacé", "CONNECTOR", at("07:42")))
        ),
        job(
            id = "job-008",
            reference = "JOB-2026-00189",
            title = "Contrôle caméra surveillance",
            description = "Réalignement de l'angle de vue de la caméra dôme d'accès principal et nettoyage objectif.",
            clientName = "Banque Commerciale",
            siteName = "Agence Gecamines",
            siteAddress = "Avenue de l'Indépendance, Immeuble BCC",
            priority = JobPriority.NORMAL,
            status = JobStatus.COMPLETED,
            scheduledAt = at("08:00"),
            startedAt = at("08:05"),
            completedAt = at("08:35"),
            checklist = checklist("job-008", listOf(
                item("c8-1", "Nettoyage dôme optique", 1, true, true),
                item("c8-2", "Ajustement zoom et focus optique", 2, true, true),
                item("c8-3", "Validation de l'enregistrement NVR", 3, true, true)
            ))
        ),
        job(
            id = "job-009",
            reference = "JOB-2026-00190",
            title = "Vérification capteurs incendie",
            description = "Test au gaz étalon des détecteurs optiques de fumée dans la salle d'archives.",
            clientName = "Gouvernorat Lualaba",
            siteName = "Archives Générales",
            siteAddress = "Place Royale, Kolwezi",
            priority = JobPriority.NORMAL,
            status = JobStatus.ASSIGNED,
            scheduledAt = at("16:15"),
            checklist = checklist("job-009", listOf(
                item("c9-1", "Passage en mode test de la centrale SSI", 1, true, false),
                item("c9-2", "Test individuel des 8 têtes de détection", 2, true, false),
                item("c9-3", "Remise en service nominale du système", 3, true, false)
            ))
        ),
        job(
            id = "job-010",
            reference = "JOB-2026-00191",
            title = "Raccordement coffret solaire",
            description = "Branchement des régulateurs MPPT et contrôle des disjoncteurs courant continu.",
            clientName = "Solaria RDC",
            siteName = "Station Relais Nord",
            siteAddress = "Colline de Musompo, Pylône GSM",
            priority = JobPriority.URGENT,
            status = JobStatus.ASSIGNED,
            scheduledAt = at("17:00"),
            checklist = checklist("job-010", listOf(
                item("c10-1", "Vérification de polarité des chaînes PV", 1, true, false),
                item("c10-2", "Serrage dynamométrique des borniers", 2, true, false),
                item("c10-3", "Enclenchement parafoudre DC", 3, true, false),
                item("c10-4", "Mesure de la tension de floating batteries", 4, true, false)
            ))
        )
    )

    private fun job(
        id: String,
        reference: String,
        title: String,
        description: String,
        clientName: String,
        siteName: String,
        siteAddress: String,
        priority: JobPriority,
        status: JobStatus,
        scheduledAt: Instant,
        startedAt: Instant? = null,
        completedAt: Instant? = null,
        syncState: SyncState = SyncState.SYNCED,
        checklist: List<ChecklistItem> = emptyList(),
        notes: List<Note> = emptyList(),
        attachments: List<Attachment> = emptyList()
    ) = Job(
        id = id,
        reference = reference,
        title = title,
        description = description,
        clientName = clientName,
        siteName = siteName,
        siteAddress = siteAddress,
        priority = priority,
        status = status,
        scheduledAt = scheduledAt,
        startedAt = startedAt,
        completedAt = completedAt,
        serverVersion = 1,
        syncState = syncState,
        createdAt = scheduledAt.minusSeconds(86_400),
        updatedAt = completedAt ?: startedAt ?: scheduledAt,
        lastSyncedAt = if (syncState == SyncState.SYNCED) initialLastSyncAt else null,
        checklist = checklist,
        notes = notes,
        attachments = attachments
    )

    private fun checklist(jobId: String, items: List<ChecklistSeed>): List<ChecklistItem> {
        return items.map {
            ChecklistItem(
                id = it.id,
                jobId = jobId,
                label = it.label,
                position = it.position,
                required = it.required,
                completed = it.completed,
                completedAt = if (it.completed) at("07:30") else null,
                serverVersion = 1,
                syncState = SyncState.SYNCED,
                updatedAt = initialLastSyncAt
            )
        }
    }

    private fun item(id: String, label: String, position: Int, required: Boolean, completed: Boolean) =
        ChecklistSeed(id, label, position, required, completed)

    private fun note(
        id: String,
        jobId: String,
        authorUserId: String,
        content: String,
        createdAt: Instant,
        syncState: SyncState = SyncState.SYNCED
    ) = Note(
        id = id,
        jobId = jobId,
        authorUserId = authorUserId,
        content = content,
        createdAt = createdAt,
        updatedAt = createdAt,
        syncState = syncState,
        serverVersion = 1,
        deletedAt = null
    )

    private fun attachment(
        id: String,
        jobId: String,
        fileName: String,
        tag: String,
        createdAt: Instant,
        syncState: SyncState = SyncState.SYNCED
    ) = Attachment(
        id = id,
        jobId = jobId,
        authorUserId = CURRENT_USER_ID,
        type = AttachmentType.PHOTO,
        localUri = "tervyn://demo/photo/$tag",
        remoteUrl = null,
        mimeType = "image/jpeg",
        fileName = fileName,
        sizeBytes = 0,
        checksumSha256 = null,
        syncState = syncState,
        createdAt = createdAt,
        uploadedAt = if (syncState == SyncState.SYNCED) initialLastSyncAt else null,
        deletedAt = null
    )

    private fun at(time: String): Instant = instant("${day}T$time:00Z")

    private fun instant(value: String): Instant = Instant.parse(value)

    private data class ChecklistSeed(
        val id: String,
        val label: String,
        val position: Int,
        val required: Boolean,
        val completed: Boolean
    )
}
