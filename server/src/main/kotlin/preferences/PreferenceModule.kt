package preferences

import org.koin.dsl.module

val preferenceModule =
    module {
        single<ListingCandidateSource> { NotesOverlapCandidateSource() }
    }
