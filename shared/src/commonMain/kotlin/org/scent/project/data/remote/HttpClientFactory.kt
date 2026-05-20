package org.scent.project.data.remote

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient
