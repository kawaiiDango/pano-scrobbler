package com.arn.scrobble

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.databinding.ContentAvdTestBinding
import com.arn.scrobble.recents.TracksVM
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Created by arn on 06/09/2017.
 */
class TestFragment : Fragment() {

    private val viewModel by viewModels<TracksVM>()
    private var _binding: ContentAvdTestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentAvdTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val avd = view.findViewById<ImageView>(R.id.test_avd).drawable as AnimatedVectorDrawable
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000)
            avd.start()
        }

        binding.testButton.text = viewModel.page.toString()
        binding.testButton.setOnClickListener {
            findNavController().navigate(R.id.prefFragment)
        }

        viewModel.page++
        // save app icon
//        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            val file = File(requireActivity().filesDir.path, "ic_launcher.png")
//            FileOutputStream(file).use {
//                ContextCompat.getDrawable(requireContext(), R.drawable.ic_launcher_for_export)!!
//                    .toBitmap(width = 512, height = 512)
//                    .compress(Bitmap.CompressFormat.PNG, 100, it)
//            }
//            withContext(Dispatchers.Main) {
//                requireContext().toast("Saved to ${file.absolutePath}")
//            }
//        }

    }
}