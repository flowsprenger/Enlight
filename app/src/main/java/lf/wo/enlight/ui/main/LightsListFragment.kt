package lf.wo.enlight.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.main_fragment.*
import lf.wo.enlight.R
import lf.wo.enlight.di.Injectable
import lf.wo.enlight.kotlin.on
import lf.wo.enlight.kotlin.toColor
import lf.wo.enlight.lifx.LightDefaults
import wo.lf.lifx.api.Light
import wo.lf.lifx.api.LightSetPowerCommand
import wo.lf.lifx.domain.PowerState
import wo.lf.lifx.extensions.fireAndForget
import javax.inject.Inject

class LightsListFragment : Fragment(), Injectable {

    companion object {
        fun newInstance() = LightsListFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: MainViewModel

    private lateinit var lightsViewModel: LightsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(MainViewModel::class.java)

        lightsViewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(LightsViewModel::class.java)

        lightsViewModel.lights.observe(this, Observer<List<Light>> { t ->
            lightsUpdated(t ?: listOf())
        })

        // to_blank.setOnClickListener{ view ->
        //     val action = MainFragmentDirections.Action_MainFragment_to_BlankFragment()
        //     view.findNavController().navigate(action)
        // }

        lights_list.apply {
            layoutManager = GridLayoutManager(context, 3)
            lights_list.adapter = LightsAdapter(listOf(), object: OnLightClicked {
                override fun onPowerClick(light: Light) {
                    LightSetPowerCommand.create(light, !light.on, LightDefaults.durationPower).fireAndForget()
                }

                override fun onClick(light: Light) {
                    val action = LightsListFragmentDirections.actionMainFragmentToBlankFragment(light.id.toString())
                    view?.findNavController()?.navigate(action)
                }
            })
        }
    }

    private fun lightsUpdated(lights: List<Light>) {
        (lights_list.adapter as LightsAdapter).lights = lights
    }

}

interface OnLightClicked{
    fun onClick(light: Light)
    fun onPowerClick(light: Light)
}

class LightsAdapter(lights: List<Light>, private val listener: OnLightClicked) : RecyclerView.Adapter<LightsAdapter.ViewHolder>() {

    var lights: List<Light> = lights
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val button: ImageButton = itemView.findViewById(R.id.imageButton)
        val label: Button = itemView.findViewById(R.id.labelText)
        val offline: View = itemView.findViewById(R.id.offline)

        fun bind(light: Light) {

            label.setOnClickListener {
                listener.onClick(light)
            }

            button.setOnClickListener {
                listener.onPowerClick(light)
            }

            drawState(light)
        }

        private fun drawState(light: Light) {
            if (light.reachable) {
                offline.visibility = View.GONE
            } else {
                offline.visibility = View.VISIBLE
            }

            if (light.power == PowerState.ON) {
                button.setImageResource(R.drawable.ic_lightbulb_on_outline)
            } else {
                button.setImageResource(R.drawable.ic_lightbulb_outline)
            }
            button.setColorFilter(light.color.toColor())

            label.text = light.label
        }

    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lights[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.light_list_item, parent, false))
    }

    override fun getItemCount(): Int = lights.size
}
