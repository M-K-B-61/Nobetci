package com.example.model

enum class TrackingState(val displayText: String) {
    INITIALIZING("Başlatılıyor..."),
    SEARCHING_VEHICLE("Hedef aranıyor..."),
    VEHICLE_LOCKED("Araç kilitlendi ✓"),
    WATCHING("Öndeki araç izleniyor"),
    POSSIBLE_MOVEMENT("Olası hareket algılandı"),
    VERIFYING_MOVEMENT("Hareket doğrulanıyor..."),
    MOVEMENT_CONFIRMED("Önünüz açıldı!"),
    CAMERA_MOVED("Cihaz hareketi saptandı"),
    STABILIZING("Sabitleniyor..."),
    TARGET_LOST("Hedef aranıyor..."),
    ALARMING("ÖNÜNÜZ AÇILDI!"),
    STOPPED("Nöbet durduruldu")
}

enum class AnalysisFpsMode(val targetFps: Int, val intervalMs: Long) {
    IDLE_WATCH(targetFps = 3, intervalMs = 330L),
    POSSIBLE_MOVEMENT(targetFps = 10, intervalMs = 100L),
    VERIFYING_MOVEMENT(targetFps = 15, intervalMs = 66L)
}

enum class Sensitivity(
    val title: String,
    val description: String,
    val motionThreshold: Float,
    val verificationFrames: Int
) {
    MINIMAL_MOVEMENT(
        title = "En Ufak Hareket",
        description = "Küçük ilerlemeleri bile anında yakalar.",
        motionThreshold = 0.22f,
        verificationFrames = 3
    ),
    SENSITIVE(
        title = "Hassas",
        description = "Kısa ilerlemelerde uyarır.",
        motionThreshold = 0.35f,
        verificationFrames = 5
    ),
    NORMAL(
        title = "Normal",
        description = "Önerilen ayar. Titreşimleri filtreleyip gerçek ilerlemeyi bekler.",
        motionThreshold = 0.50f,
        verificationFrames = 7
    )
}
