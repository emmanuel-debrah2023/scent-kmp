@file:Suppress("ktlint:standard:function-naming") // iOS factory functions follow UIViewController PascalCase convention

package org.scent.project

import App
import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App() }
