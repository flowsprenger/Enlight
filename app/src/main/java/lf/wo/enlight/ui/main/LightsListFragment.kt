/*

  Copyright (c) 2018 Florian Sprenger

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

 */

package lf.wo.enlight.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.main_fragment.*
import lf.wo.enlight.R
import lf.wo.enlight.kotlin.on
import lf.wo.enlight.kotlin.toColor
import lf.wo.enlight.lifx.LightDefaults
import lf.wo.enlight.viewmodel.LightsViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import wo.lf.lifx.api.*
import wo.lf.lifx.domain.PowerState
import wo.lf.lifx.extensions.fireAndForget

class LightsListFragment : Fragment() {

    private val lightsViewModel: LightsViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        lightsViewModel.locations.observe(this, Observer<List<Location>> { t ->
            lightsUpdated(t.flatToHierarchy() ?: listOf())
        })

        lightsViewModel.lights.observe(this, Observer<List<Light>> { t ->
            lightsUpdated(lightsViewModel.locations.value?.flatToHierarchy() ?: listOf())
        })

        lights_list.apply {
            layoutManager = GridLayoutManager(context, 3)
            lights_list.adapter = LightsAdapter(listOf(), object : OnLightClicked {
                override fun onPowerClick(entity: LifxEntity) {
                    entity.lights.forEach { light -> LightSetPowerCommand.create(light, !light.on, LightDefaults.durationPower).fireAndForget() }
                }

                override fun onClick(light: Light) {
                    val action = LightsListFragmentDirections.actionMainFragmentToBlankFragment(light.id.toString())
                    view?.findNavController()?.navigate(action)
                }
            })
        }
    }

    private fun lightsUpdated(lights: List<LifxEntity>) {
        (lights_list.adapter as LightsAdapter).lights = lights
    }

}

private fun List<Location>.flatToHierarchy(): List<LifxEntity>? {
    return flatMap { listOf(it) + it.groups.flatMap { listOf(it as LifxEntity) + it.lights.map { it as LifxEntity } } }
}

interface OnLightClicked {
    fun onClick(light: Light)
    fun onPowerClick(entity: LifxEntity)
}

class LightsAdapter(lights: List<LifxEntity>, private val listener: OnLightClicked) : RecyclerView.Adapter<LightsAdapter.ViewHolder>() {

    var lights: List<LifxEntity> = lights
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val button: ImageButton = itemView.findViewById(R.id.imageButton)
        val label: Button = itemView.findViewById(R.id.labelText)
        val offline: View = itemView.findViewById(R.id.offline)

        fun bindLight(light: Light) {

            label.setOnClickListener {
                listener.onClick(light)
            }

            button.setOnClickListener {
                listener.onPowerClick(light)
            }

            drawLightState(light)
        }

        private fun drawLightState(light: Light) {
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

        fun bindGroup(group: Group) {

            button.setOnClickListener {
                listener.onPowerClick(group)
            }

            drawGroupState(group)
        }

        private fun drawGroupState(group: Group) {
            if (group.lights.any { it.reachable }) {
                offline.visibility = View.GONE
            } else {
                offline.visibility = View.VISIBLE
            }

            if (group.lights.any { it.power == PowerState.ON }) {
                button.setImageResource(R.drawable.ic_group_on)
            } else {
                button.setImageResource(R.drawable.ic_group_off)
            }

            label.text = group.name
        }

        fun bindLocation(location: Location) {

            button.setOnClickListener {
                listener.onPowerClick(location)
            }

            drawLocationState(location)
        }

        private fun drawLocationState(location: Location) {
            if (location.lights.any { it.reachable }) {
                offline.visibility = View.GONE
            } else {
                offline.visibility = View.VISIBLE
            }

            if (location.lights.any { it.power == PowerState.ON }) {
                button.setImageResource(R.drawable.ic_location_on)
            } else {
                button.setImageResource(R.drawable.ic_location_off)
            }

            label.text = location.name
        }

    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entity = lights[position]
        when (entity) {
            is Light -> holder.bindLight(entity)
            is Group -> holder.bindGroup(entity)
            is Location -> holder.bindLocation(entity)
        }

    }

    override fun getItemViewType(position: Int): Int {
        val entity = lights[position]
        return when (entity) {
            is Location -> R.layout.light_list_item_location
            is Group -> R.layout.light_list_item_group
            is Light -> R.layout.light_list_item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.light_list_item, parent, false))
    }

    override fun getItemCount(): Int = lights.size
}
