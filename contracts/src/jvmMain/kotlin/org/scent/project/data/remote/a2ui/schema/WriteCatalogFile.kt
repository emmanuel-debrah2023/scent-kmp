package org.scent.project.data.remote.a2ui.schema

import java.io.File

// Build tool entry point: writes the generated scent/v1 catalog schema to the path in args[0].
fun main(args: Array<String>) {
    val target = File(requireNotNull(args.firstOrNull()) { "usage: WriteCatalogFile <output file>" })
    target.parentFile?.mkdirs()
    target.writeText(catalogSchemaJson() + "\n")
}
