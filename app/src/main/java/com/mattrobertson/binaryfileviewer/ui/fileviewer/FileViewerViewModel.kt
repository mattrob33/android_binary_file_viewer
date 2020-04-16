package com.mattrobertson.binaryfileviewer.ui.fileviewer

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.mattrobertson.binaryfileviewer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.NumberFormatException
import kotlin.text.Typography.nbsp

class FileViewerViewModel : ViewModel() {

    enum class DisplayMode {
        DECIMAL,
        HEX
    }

    private var mPrefs: SharedPreferences? = null
    private var mAppContext: Context? = null

    private var mIgnoreTextColor = Color.LTGRAY

    private var mDisplayMode = DisplayMode.DECIMAL
    private var mCellSize = 3

    var mBytesPerLine = 8
        private set

    private var mFileBytes = ByteArray(0)

    private val mFilePath = "/storage/emulated/0/Android/data/com.accordancebible.accordance/files/Accordance/UserFilesParent/Highlights/My Mobile Highlights.hlt"

    private val _mTitleString = MutableLiveData<String>()
    val mTitleString: LiveData<String>
        get() = _mTitleString

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
    fun start(appContext: Context) {
        mAppContext = appContext
        refreshFileRead()
    }

    private fun setupDisplay() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mAppContext)
        val dimIgnore = mPrefs?.getBoolean("dim_zeroes", true) ?: true
        val prefsDisplayMode = mPrefs?.getString("binary_format", "dec")
        val prefsBytesPerLine = mPrefs?.getString("bytes_per_line", "8") ?: "8"

        mIgnoreTextColor = if (dimIgnore) mAppContext!!.getColor(R.color.ignoreTextColor) else mAppContext!!.getColor(R.color.primaryTextColor)

        mDisplayMode = when (prefsDisplayMode) {
            "dec" -> DisplayMode.DECIMAL
            "hex" -> DisplayMode.HEX
            else -> DisplayMode.DECIMAL
        }

        mBytesPerLine = try { Integer.parseInt(prefsBytesPerLine) } catch (e: NumberFormatException) { 8 }

        mCellSize = when(mDisplayMode) {
            DisplayMode.DECIMAL -> 3
            DisplayMode.HEX -> 2
        }
    }

    @ExperimentalUnsignedTypes
    fun refreshFileRead() {
        _mTitleString.value = mFilePath.substring(mFilePath.lastIndexOf(File.separator) + 1)
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
            val numRows = mFileBytes.size / mBytesPerLine
            for (i in 0..numRows) {
                sb.append(i * mBytesPerLine)
                if (i < numRows)
                    sb.append(System.lineSeparator())
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

                if (index != 0 && (index + 1) % mBytesPerLine == 0)
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

            if (index != 0 && (index + 1) % mBytesPerLine == 0)
                sb.append(System.lineSeparator())
            else
                sb.append(nbsp)
        }
        return sb
    }
}
