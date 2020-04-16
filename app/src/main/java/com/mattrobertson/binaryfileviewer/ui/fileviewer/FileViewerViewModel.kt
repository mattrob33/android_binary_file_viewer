package com.mattrobertson.binaryfileviewer.ui.fileviewer

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattrobertson.binaryfileviewer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class FileViewerViewModel : ViewModel() {

    enum class DisplayMode {
        DECIMAL,
        HEX
    }

    private var mIgnoreTextColor = Color.LTGRAY

    private var mDisplayMode = DisplayMode.DECIMAL
    private var mCellSize = 3

    var mRowSize = 8
        private set

    private var mFileBytes = ByteArray(0)

    private val mFilePath = "/storage/emulated/0/Android/data/com.accordancebible.accordance/files/Accordance/UserFilesParent/Highlights/My Mobile Highlights.hlt"

    private val _mBytesStyledString = MutableLiveData<SpannableStringBuilder>()
    val mBytesStyledString: LiveData<SpannableStringBuilder>
        get() = _mBytesStyledString

    private val _mPosString = MutableLiveData<String>()
    val mPosString: LiveData<String>
        get() = _mPosString

    private val _mAsciiStyledString = MutableLiveData<SpannableStringBuilder>()
    val mAsciiStyledString: LiveData<SpannableStringBuilder>
        get() = _mAsciiStyledString

    @ExperimentalUnsignedTypes
    fun start(ctx: Context) {
        mIgnoreTextColor = ctx.getColor(R.color.ignoreTextColor)
        refreshFileRead()
    }

    private fun setupDisplay() {
        mCellSize = when(mDisplayMode) {
            DisplayMode.DECIMAL -> 3
            DisplayMode.HEX -> 2
        }
    }

    @ExperimentalUnsignedTypes
    fun refreshFileRead() {
        viewModelScope.launch(Dispatchers.IO) {
            readFile()
            refreshDisplay()
        }
    }

    @ExperimentalUnsignedTypes
    fun refreshDisplay() {
        setupDisplay()

        viewModelScope.launch(Dispatchers.IO) {
            val bytesString = byteArrayToString(mFileBytes)
            viewModelScope.launch(Dispatchers.Main) {
                _mBytesStyledString.value = bytesString
            }

            val sb = StringBuilder()
            val numRows = mFileBytes.size / mRowSize
            for (i in 0..numRows) {
                sb.append(i * mRowSize).append(System.lineSeparator())
            }
            viewModelScope.launch(Dispatchers.Main) {
                _mPosString.value = sb.toString()
            }

            val spanSb = SpannableStringBuilder()
            mFileBytes.forEachIndexed { index, byte ->
                if (byte in 32..126)
                    spanSb.append(byte.toChar())
                else {
                    spanSb.append("·")
                    spanSb.setSpan(ForegroundColorSpan(mIgnoreTextColor), spanSb.length - 1, spanSb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (index != 0 && (index + 1) % mRowSize == 0)
                    spanSb.append(System.lineSeparator())
            }
            viewModelScope.launch(Dispatchers.Main) {
                _mAsciiStyledString.value = spanSb
            }
        }
    }

    private fun readFile() {
        mFileBytes = File(mFilePath).readBytes()
    }

    @ExperimentalUnsignedTypes
    private fun byteArrayToString(arr: ByteArray): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        var sByte: String
        arr.forEachIndexed { index, byte ->
            sByte = when(mDisplayMode) {
                DisplayMode.DECIMAL -> byte.toUByte().toString()
                DisplayMode.HEX -> String.format("%02X", byte)
            }

            while (sByte.length < mCellSize)
                sByte = "0$sByte"
            sb.append(sByte)

            if (byte.compareTo(0) == 0)
                sb.setSpan(ForegroundColorSpan(mIgnoreTextColor), sb.length - sByte.length, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            if (index != 0 && (index + 1) % mRowSize == 0)
                sb.append(System.lineSeparator())
            else
                sb.append(" ")
        }
        return sb
    }
}
