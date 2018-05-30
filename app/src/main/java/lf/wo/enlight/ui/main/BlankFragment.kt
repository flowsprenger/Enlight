package lf.wo.enlight.ui.main

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import lf.wo.enlight.R


class BlankFragment : Fragment() {

    companion object {
        fun newInstance() = BlankFragment()
    }

    private lateinit var viewModel: BlankViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.blank_fragment, container, false)
    }


    private lateinit var lightsViewModel: LightsViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BlankViewModel::class.java)
        lightsViewModel = ViewModelProviders.of(activity!!).get(LightsViewModel::class.java)

        arguments?.let {
            val id = it.getLong("lightId")
            val light =lightsViewModel.lights.value?.find { it.id == id }
            print(light)
        }

        // dagger hmm can we annotate some properties and only pass the config to the constructor?
    }

}
