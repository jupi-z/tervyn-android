package dev.amenokizele.tervyn.demo

import dev.amenokizele.tervyn.model.DemoChecklistItem
import dev.amenokizele.tervyn.model.DemoJob
import dev.amenokizele.tervyn.model.DemoNote
import dev.amenokizele.tervyn.model.DemoPhoto
import dev.amenokizele.tervyn.model.JobPriority
import dev.amenokizele.tervyn.model.JobStatus
import dev.amenokizele.tervyn.model.SyncState

object DemoData {
    val initialJobs: List<DemoJob> = listOf(
        DemoJob(
            id = "job-001",
            reference = "JOB-2026-00182",
            title = "Installation routeur",
            description = "Déploiement d'un nouveau routeur d'entreprise et raccordement au switch principal de la baie de brassage.",
            clientName = "Alpha Distribution",
            siteName = "Kolwezi Centre",
            siteAddress = "14 Avenue des Usines, Quartier Industriel, Kolwezi",
            priority = JobPriority.HIGH,
            status = JobStatus.ASSIGNED,
            scheduledAt = "08:30",
            syncState = SyncState.SYNCED,
            checklist = listOf(
                DemoChecklistItem("c1-1", "Vérifier l'alimentation", 1, required = true, completed = false),
                DemoChecklistItem("c1-2", "Inspecter le câblage", 2, required = true, completed = false),
                DemoChecklistItem("c1-3", "Configurer le routeur", 3, required = true, completed = false),
                DemoChecklistItem("c1-4", "Tester la connexion", 4, required = false, completed = false)
            ),
            notes = listOf(
                DemoNote("n1-1", "Prévoir un câble RJ45 Cat6 de 5 mètres supplémentaire.", "Support N1", "07:45", SyncState.SYNCED)
            ),
            photos = emptyList()
        ),
        DemoJob(
            id = "job-002",
            reference = "JOB-2026-00183",
            title = "Inspection groupe électrogène",
            description = "Contrôle préventif trimestriel, vérification des niveaux d'huile, filtres et test de démarrage autonome.",
            clientName = "Horizon Depot",
            siteName = "Hangar Logistique B",
            siteAddress = "Route Nationale 39, Km 12, Kolwezi",
            priority = JobPriority.NORMAL,
            status = JobStatus.ASSIGNED,
            scheduledAt = "09:45",
            syncState = SyncState.SYNCED,
            checklist = listOf(
                DemoChecklistItem("c2-1", "Vérifier niveau de liquide de refroidissement", 1, required = true, completed = false),
                DemoChecklistItem("c2-2", "Contrôler batterie de démarrage", 2, required = true, completed = false),
                DemoChecklistItem("c2-3", "Essai de basculement source normale/secours", 3, required = true, completed = false),
                DemoChecklistItem("c2-4", "Nettoyage filtre à air", 4, required = false, completed = false)
            ),
            notes = emptyList(),
            photos = emptyList()
        ),
        DemoJob(
            id = "job-003",
            reference = "JOB-2026-00184",
            title = "Maintenance climatisation",
            description = "Nettoyage des échangeurs thermiques et recharge fluide frigorigène salle serveurs.",
            clientName = "Kivu Services",
            siteName = "Datacenter Est",
            siteAddress = "88 Boulevard de la Libération, Lubumbashi",
            priority = JobPriority.HIGH,
            status = JobStatus.IN_PROGRESS,
            scheduledAt = "11:00",
            startedAt = "11:05",
            syncState = SyncState.PENDING,
            checklist = listOf(
                DemoChecklistItem("c3-1", "Relevé températures entrée / sortie", 1, required = true, completed = true),
                DemoChecklistItem("c3-2", "Inspection de l'évacuation des condensats", 2, required = true, completed = true),
                DemoChecklistItem("c3-3", "Remplacement des filtres anti-poussière", 3, required = true, completed = false),
                DemoChecklistItem("c3-4", "Test des sondes d'hygrométrie", 4, required = false, completed = false)
            ),
            notes = listOf(
                DemoNote("n3-1", "Filtre primaire fortement encrassé par les vents de sable.", "Vous", "11:15", SyncState.PENDING)
            ),
            photos = listOf(
                DemoPhoto("p3-1", "Filtre salle serveurs", "FILTER", "11:18", SyncState.PENDING)
            )
        ),
        DemoJob(
            id = "job-004",
            reference = "JOB-2026-00185",
            title = "Audit câblage réseau",
            description = "Recette et certification réflectométrie optique sur la dorsale inter-bâtiments.",
            clientName = "Lualaba Office",
            siteName = "Bâtiment Administratif",
            siteAddress = "Avenue Kasavubu, Immeuble Horizon, Kolwezi",
            priority = JobPriority.NORMAL,
            status = JobStatus.ASSIGNED,
            scheduledAt = "13:30",
            syncState = SyncState.SYNCED,
            checklist = listOf(
                DemoChecklistItem("c4-1", "Repérage des tiroirs optiques", 1, required = true, completed = false),
                DemoChecklistItem("c4-2", "Test d'atténuation sur 12 brins", 2, required = true, completed = false),
                DemoChecklistItem("c4-3", "Étiquetage des jarretières", 3, required = false, completed = false)
            ),
            notes = emptyList(),
            photos = emptyList()
        ),
        DemoJob(
            id = "job-005",
            reference = "JOB-2026-00186",
            title = "Remplacement onduleur",
            description = "Remplacement d'urgence de l'onduleur 3kVA en défaut batterie sur le standard téléphonique.",
            clientName = "Mamba Logistics",
            siteName = "Quai de chargement",
            siteAddress = "Zone Portuaire Fluviale, Bâtiment 4",
            priority = JobPriority.URGENT,
            status = JobStatus.ASSIGNED,
            scheduledAt = "14:15",
            syncState = SyncState.SYNCED,
            checklist = listOf(
                DemoChecklistItem("c5-1", "Consignation électrique du coffret", 1, required = true, completed = false),
                DemoChecklistItem("c5-2", "Dépose de l'ancien onduleur HS", 2, required = true, completed = false),
                DemoChecklistItem("c5-3", "Pose et raccordement du nouvel onduleur", 3, required = true, completed = false),
                DemoChecklistItem("c5-4", "Test de décharge sur charge résistive", 4, required = true, completed = false)
            ),
            notes = emptyList(),
            photos = emptyList()
        ),
        DemoJob(
            id = "job-006",
            reference = "JOB-2026-00187",
            title = "Mise en service imprimante",
            description = "Installation d'un copieur multifonction réseau et intégration à l'annuaire d'entreprise.",
            clientName = "Nova Trading",
            siteName = "Agence Centrale",
            siteAddress = "25 Avenue Laurent Kabila, Kolwezi",
            priority = JobPriority.LOW,
            status = JobStatus.ASSIGNED,
            scheduledAt = "15:30",
            syncState = SyncState.SYNCED,
            checklist = listOf(
                DemoChecklistItem("c6-1", "Déballage et fixation des bacs papier", 1, required = true, completed = false),
                DemoChecklistItem("c6-2", "Attribution IP statique et VLAN bureautique", 2, required = true, completed = false),
                DemoChecklistItem("c6-3", "Impression de page de test calibrée", 3, required = true, completed = false)
            ),
            notes = emptyList(),
            photos = emptyList()
        ),
        DemoJob(
            id = "job-007",
            reference = "JOB-2026-00188",
            title = "Dépannage borne Wi-Fi",
            description = "Perte de signal récurrente dans l'atelier mécanique, vérifier PoE et switch d'étage.",
            clientName = "Gecamines Mines",
            siteName = "Atelier Mécanique Sud",
            siteAddress = "Route d'Uvira, Site Minier 2",
            priority = JobPriority.HIGH,
            status = JobStatus.COMPLETED,
            scheduledAt = "07:00",
            startedAt = "07:05",
            completedAt = "07:55",
            syncState = SyncState.SYNCED,
            checklist = listOf(
                DemoChecklistItem("c7-1", "Contrôle tension injecteur PoE", 1, required = true, completed = true),
                DemoChecklistItem("c7-2", "Test câble RJ45 blindé", 2, required = true, completed = true),
                DemoChecklistItem("c7-3", "Mise à jour firmware AP", 3, required = true, completed = true)
            ),
            notes = listOf(
                DemoNote("n7-1", "Connecteur RJ45 oxydé par l'humidité, re-serti à neuf.", "Vous", "07:40", SyncState.SYNCED)
            ),
            photos = listOf(
                DemoPhoto("p7-1", "Connecteur remplacé", "CONNECTOR", "07:42", SyncState.SYNCED)
            )
        ),
        DemoJob(
            id = "job-008",
            reference = "JOB-2026-00189",
            title = "Contrôle caméra surveillance",
            description = "Réalignement de l'angle de vue de la caméra dôme d'accès principal et nettoyage objectif.",
            clientName = "Banque Commerciale",
            siteName = "Agence Gecamines",
            siteAddress = "Avenue de l'Indépendance, Immeuble BCC",
            priority = JobPriority.NORMAL,
            status = JobStatus.COMPLETED,
            scheduledAt = "08:00",
            startedAt = "08:05",
            completedAt = "08:35",
            syncState = SyncState.SYNCED,
            checklist = listOf(
                DemoChecklistItem("c8-1", "Nettoyage dôme optique", 1, required = true, completed = true),
                DemoChecklistItem("c8-2", "Ajustement zoom et focus optique", 2, required = true, completed = true),
                DemoChecklistItem("c8-3", "Validation de l'enregistrement NVR", 3, required = true, completed = true)
            ),
            notes = emptyList(),
            photos = emptyList()
        ),
        DemoJob(
            id = "job-009",
            reference = "JOB-2026-00190",
            title = "Vérification capteurs incendie",
            description = "Test au gaz étalon des détecteurs optiques de fumée dans la salle d'archives.",
            clientName = "Gouvernorat Lualaba",
            siteName = "Archives Générales",
            siteAddress = "Place Royale, Kolwezi",
            priority = JobPriority.NORMAL,
            status = JobStatus.ASSIGNED,
            scheduledAt = "16:15",
            syncState = SyncState.SYNCED,
            checklist = listOf(
                DemoChecklistItem("c9-1", "Passage en mode test de la centrale SSI", 1, required = true, completed = false),
                DemoChecklistItem("c9-2", "Test individuel des 8 têtes de détection", 2, required = true, completed = false),
                DemoChecklistItem("c9-3", "Remise en service nominale du système", 3, required = true, completed = false)
            ),
            notes = emptyList(),
            photos = emptyList()
        ),
        DemoJob(
            id = "job-010",
            reference = "JOB-2026-00191",
            title = "Raccordement coffret solaire",
            description = "Branchement des régulateurs MPPT et contrôle des disjoncteurs courant continu.",
            clientName = "Solaria RDC",
            siteName = "Station Relais Nord",
            siteAddress = "Colline de Musompo, Pylône GSM",
            priority = JobPriority.URGENT,
            status = JobStatus.ASSIGNED,
            scheduledAt = "17:00",
            syncState = SyncState.SYNCED,
            checklist = listOf(
                DemoChecklistItem("c10-1", "Vérification de polarité des chaînes PV", 1, required = true, completed = false),
                DemoChecklistItem("c10-2", "Serrage dynamométrique des borniers", 2, required = true, completed = false),
                DemoChecklistItem("c10-3", "Enclenchement parafoudre DC", 3, required = true, completed = false),
                DemoChecklistItem("c10-4", "Mesure de la tension de floating batteries", 4, required = true, completed = false)
            ),
            notes = emptyList(),
            photos = emptyList()
        )
    )
}
