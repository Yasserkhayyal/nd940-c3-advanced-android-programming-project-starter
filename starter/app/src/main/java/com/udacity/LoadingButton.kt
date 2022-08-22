package com.udacity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.udacity.models.ButtonState
import kotlin.properties.Delegates

const val BUTTON_LOADING_ANIMATION_DURATION = 5000L

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var widthSize = 0
    private var heightSize = 0
    private var textWidth = 0f

    private var textSize: Float = resources.getDimension(R.dimen.sp_20)
    private var circleXOffset = textSize / 2

    private var buttonTitle: String

    private var progressWidth = 0f
    private var progressCircle = 0f

    private var buttonBackground = ContextCompat.getDrawable(context, R.drawable.loading_button_bg)
    private var loadingColor = ContextCompat.getColor(context, R.color.colorPrimaryDark)
    private var circleColor = ContextCompat.getColor(context, R.color.colorAccent)

    private var valueAnimator = ValueAnimator()

    private val paint = Paint().apply {
        isAntiAlias = true
        textSize = resources.getDimension(R.dimen.sp_20)
    }

    var buttonState: ButtonState by Delegates.observable(ButtonState.Completed) { _, _, newState ->
        when (newState) {
            ButtonState.Clicked -> {
                buttonTitle = context.getString(R.string.button_clicked)
                invalidate()
            }
            ButtonState.Loading -> {
                valueAnimator = ValueAnimator.ofFloat(0f, widthSize.toFloat())
                valueAnimator.duration = BUTTON_LOADING_ANIMATION_DURATION
                valueAnimator.addUpdateListener { animation ->
                    buttonTitle = context.getString(R.string.button_loading)
                    progressWidth = animation.animatedValue as Float
                    progressCircle = (progressWidth / widthSize) * 360
                    invalidate()
                }
                valueAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animator: Animator) {
                        if (newState == ButtonState.Loading) {
                            valueAnimator.start()//keep loading as long as download still incomplete
                        } else {
                            resetButtonUI(animator as ValueAnimator, newState)
                        }
                    }

                    override fun onAnimationCancel(animator: Animator) {
                        resetButtonUI(animator as ValueAnimator, newState)
                    }
                })
                valueAnimator.start()
            }
            is ButtonState.Completed -> {
                valueAnimator.cancel()
                resetButtonUI(valueAnimator, newState)
            }
            ButtonState.NetworkUnavailable -> {
                valueAnimator.cancel()
                resetButtonUI(valueAnimator, newState)
            }
            ButtonState.Failed -> {
                valueAnimator.cancel()
                resetButtonUI(valueAnimator, newState)
            }
        }
    }

    init {
        buttonTitle = context.getString(R.string.button_download)
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            buttonBackground =
                getDrawable(R.styleable.LoadingButton_buttonColor) ?: ContextCompat.getDrawable(
                    context,
                    R.drawable.loading_button_bg
                )
            background = buttonBackground
            loadingColor = getColor(
                R.styleable.LoadingButton_buttonLoadingColor,
                ContextCompat.getColor(context, R.color.colorPrimaryDark)
            )
            circleColor = getColor(
                R.styleable.LoadingButton_loadingCircleColor,
                ContextCompat.getColor(context, R.color.colorAccent)
            )
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            drawBackgroundColor(canvas)
            drawProgressBackground(canvas)
            drawTitle(canvas)
            drawCircleProgress(canvas)
        }
    }

    private fun resetButtonUI(animator: ValueAnimator, newState: ButtonState) {
        animator.removeAllUpdateListeners()
        progressWidth = 0f
        progressCircle = 0f
        buttonTitle = when (newState) {
            ButtonState.NetworkUnavailable -> {
                isEnabled = false
                context.getString(R.string.button_network_unavailable)
            }
            ButtonState.Failed -> {
                isEnabled = false
                context.getString(R.string.button_failed)
            }
            else -> {
                isEnabled = true
                context.getString(R.string.button_download)
            }
        }
        invalidate()
    }

    private fun drawBackgroundColor(canvas: Canvas) {
        paint.color = drawableState[0]
        canvas.drawRect(0f, 0f, widthSize.toFloat(), heightSize.toFloat(), paint)
    }

    private fun drawProgressBackground(canvas: Canvas) {
        paint.color = loadingColor
        canvas.drawRect(0f, 0f, progressWidth, heightSize.toFloat(), paint)
    }

    private fun drawCircleProgress(canvas: Canvas) {
        canvas.save()
        //translate the canvas to the target x and w coordinates
        canvas.translate(
            widthSize / 2 + textWidth / 2 + circleXOffset,
            heightSize / 2 - textSize / 2
        )
        paint.color = circleColor
        //draw the arc that composes the circle
        canvas.drawArc(RectF(0f, 0f, textSize, textSize), 0F, progressCircle, true, paint)
        canvas.restore()
    }

    private fun drawTitle(canvas: Canvas) {
        paint.color = Color.WHITE
        textWidth = paint.measureText(buttonTitle)
        canvas.drawText(
            buttonTitle,
            widthSize / 2 - textWidth / 2,
            heightSize / 2 - (paint.descent() + paint.ascent()) / 2,
            paint
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }

}