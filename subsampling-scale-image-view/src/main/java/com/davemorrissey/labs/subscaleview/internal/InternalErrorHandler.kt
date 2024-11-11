package com.davemorrissey.labs.subscaleview.internal

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class InternalErrorHandler : AbstractCoroutineContextElement(CoroutineExceptionHandler),
	CoroutineExceptionHandler {

	override fun handleException(context: CoroutineContext, exception: Throwable) {
		if (SubsamplingScaleImageView.isDebug) {
			exception.printStackTrace()
		}
	}
}
