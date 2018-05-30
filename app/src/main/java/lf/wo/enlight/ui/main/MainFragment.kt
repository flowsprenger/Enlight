package lf.wo.enlight.ui.main

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.main_fragment.*
import lf.wo.enlight.R
import lf.wo.enlight.R.id.butt
import lf.wo.enlight.R.id.imageButton
import wo.lf.lifx.api.Light

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    private lateinit var lightsViewModel: LightsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        lightsViewModel = ViewModelProviders.of(activity!!).get(LightsViewModel::class.java)
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
                override fun onClick(light: Light) {
                    val action = MainFragmentDirections.Action_MainFragment_to_BlankFragment()
                    action.arguments?.putLong("lightId", light.id)
                    view!!.findNavController().navigate(R.id.action_MainFragment_to_BlankFragment, Bundle().apply { putLong("lightId", light.id)})
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
}

class LightsAdapter(lights: List<Light>, private val listener: OnLightClicked) : RecyclerView.Adapter<LightsAdapter.ViewHolder>() {

    var lights: List<Light> = lights
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val x = itemView.findViewById<ImageButton>(imageButton)

        fun bind(light: Light) {
            x.setOnClickListener {
                listener.onClick(light)
            }
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
