package com.mattrobertson.binaryfileviewer.ui.fileviewer

import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mattrobertson.binaryfileviewer.BottomNavigationDrawerFragment
import com.mattrobertson.binaryfileviewer.R
import kotlinx.android.synthetic.main.file_viewer_fragment.*
import java.lang.Exception
import java.lang.NumberFormatException


class FileViewerFragment : Fragment() {

    companion object {
        fun newInstance() = FileViewerFragment()
    }

    private val mViewModel: FileViewerViewModel by lazy {
        ViewModelProvider(this).get(FileViewerViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.file_viewer_fragment, container, false)
    }

    @ExperimentalUnsignedTypes
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupView()
        observeViewModel()
        mViewModel.start(activity!!)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.action_menu, menu)
    }

    @ExperimentalUnsignedTypes
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val bottomNavDrawerFragment = BottomNavigationDrawerFragment()
                bottomNavDrawerFragment.show(activity!!.supportFragmentManager, bottomNavDrawerFragment.tag)
                true
            }
            R.id.app_bar_refresh -> {
                mViewModel.refreshFileRead()
                true
            }
            R.id.app_bar_goto -> {
                showGotoDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupView() {
        tv_bytes.setOnClickListener {

        }
    }

    private fun observeViewModel() {
        mViewModel.mPosString.observe(viewLifecycleOwner) {
            tv_position.text = it
        }

        mViewModel.mBytesStyledString.observe(viewLifecycleOwner) {
            tv_bytes.text = it
        }

        mViewModel.mAsciiStyledString.observe(viewLifecycleOwner) {
            tv_ascii.text = it
        }
    }

    private fun showGotoDialog() {
        val etInput = EditText(activity)
        etInput.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        etInput.inputType = InputType.TYPE_CLASS_NUMBER
        etInput.hint = "0"

        MaterialAlertDialogBuilder(activity!!)
            .setTitle("Go to")
            .setView(etInput)
            .setPositiveButton("Go") { _, _ ->
                val lineNum = try {
                    Integer.parseInt(etInput.text.toString()) / mViewModel.mRowSize
                } catch (e: NumberFormatException) {
                    0
                }
                scrollToLine(lineNum)
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .show()
    }

    private fun scrollToLine(lineNum: Int) {
        file_viewer_scroller.post {
            var actualLineNum = lineNum
            if (actualLineNum > tv_bytes.lineCount)
                actualLineNum = tv_bytes.lineCount
            val y = tv_bytes.layout.getLineTop(actualLineNum)
            file_viewer_scroller.scrollTo(0, y)
        }
    }
}