package preferences

import data.schema.FragranceNotesTable
import org.jetbrains.exposed.v1.jdbc.select
import org.scent.project.domain.util.Result

/**
 * Every note any fragrance has, lowercase. It is wider than [ScentFamily]'s notes on purpose: a
 * 'why' line naming an off-set note (say oud on a fragrance without it) must still be caught.
 */
fun interface NoteVocabulary {
    suspend fun notes(): Result<Set<String>>
}

class StoredNoteVocabulary : NoteVocabulary {
    override suspend fun notes(): Result<Set<String>> =
        readFromStorage {
            FragranceNotesTable
                .select(FragranceNotesTable.note)
                .withDistinct()
                .map { it[FragranceNotesTable.note].trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toSet()
        }
}
