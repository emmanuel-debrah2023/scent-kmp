package ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Either

abstract class BaseViewModel : ViewModel() {

    private val _error = MutableSharedFlow<AppError>()
    val error: SharedFlow<AppError> = _error.asSharedFlow()

    protected fun handleError(error: AppError) {
        viewModelScope.launch {
            _error.emit(error)
        }
    }

    protected fun <T> Either<AppError, T>.handleResult(
        onSuccess: (T) -> Unit,
        onError: ((AppError) -> Unit)? = null
    ) {
        fold(
            ifLeft = { error ->
                onError?.invoke(error) ?: handleError(error)
            },
            ifRight = { data ->
                onSuccess(data)
            }
        )
    }
}
