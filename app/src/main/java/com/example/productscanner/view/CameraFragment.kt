package com.example.productscanner.view

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController

import com.example.productscanner.R
import com.example.productscanner.databinding.CameraFragmentBinding
import com.example.productscanner.viewmodel.*
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult

class CameraFragment : Fragment() {

    private lateinit var viewModel: CameraViewModel
    private val viewModelShared by activityViewModels<MainActivityViewModel>()
    private lateinit var binding: CameraFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.scanner)

        binding = CameraFragmentBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.cameraView.setLifecycleOwner(viewLifecycleOwner)

        viewModel = ViewModelProviders.of(this).get(CameraViewModel::class.java)
        viewModel.setProducts(viewModelShared.products)
        binding.viewModel = viewModel

        hidePreview()

        binding.btnScanUpc.setOnClickListener {
            viewModel.typeScanner = TypeScanner.UPC
            binding.cameraView.takePicture()
            viewModel.hideButtons()
        }

        binding.btnScanSku.setOnClickListener {
            viewModel.typeScanner = TypeScanner.SKU
            binding.cameraView.takePicture()
            viewModel.hideButtons()
        }

        binding.cameraView.addCameraListener(object: CameraListener(){
            @Override
            override fun onPictureTaken(result: PictureResult) {
                Log.d("Camera View", "Picture taken")
                viewModel.getBitmap(result.data)

            }
        })

        setObservers()

        return binding.root
    }

    private fun setObservers(){
        // The scanner was successful
        viewModel.scannerStatusItem.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let {scannerStatusItem ->
                when(scannerStatusItem){
                    ScannerStatusItem.FOUND -> {
                        viewModel.productByBarCode?.let { product ->
                            this.findNavController()
                                .navigate(CameraFragmentDirections
                                    .actionCameraxToDetailProduct(product))
                        }
                    }
                    ScannerStatusItem.NOT_FOUND -> {
                        Toast.makeText(context, "Item not found",Toast.LENGTH_LONG).show()
                        hidePreview()
                    }
                }
            }
        })

        // If the scanner fail
        viewModel.scannerStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { scannerStatus ->
                when(scannerStatus){
                    ScannerStatus.TRY_AGAIN -> {
                        tryAgain()
                    }
                    ScannerStatus.FAIL -> {
                        onFail()
                    }
                }
            }
        })

        viewModel.bitMap.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.imagePreview.setImageBitmap(it)
                showPreview()
            }
        })
    }

    private fun onFail() {
        Toast.makeText(
            context,
            "Sorry, something went wrong!",
            Toast.LENGTH_LONG
        ).show()
        hidePreview()
    }

    private fun tryAgain() {
        Toast.makeText(
            activity, "Try again", Toast.LENGTH_SHORT
        ).show()
        hidePreview()
    }

    private fun showPreview() {
        binding.imagePreview.visibility = View.VISIBLE
        binding.cameraView.visibility = View.GONE
    }

    private fun hidePreview() {
        binding.imagePreview.visibility = View.GONE
        binding.cameraView.visibility = View.VISIBLE
        viewModel.showButtons()
    }
}
