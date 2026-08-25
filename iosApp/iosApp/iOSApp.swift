import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    // Koin must be started before any composition runs, since App() resolves
    // SessionViewModel via koinViewModel(). This is the iOS counterpart to
    // ScentApplication.onCreate() on Android — once per process launch, not
    // per view controller, as startKoin throws if called twice.
    init() {
        HelperKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}