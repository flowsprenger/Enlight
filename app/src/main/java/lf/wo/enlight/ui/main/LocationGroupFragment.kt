package lf.wo.enlight.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_location_group.*
import lf.wo.enlight.R
import lf.wo.enlight.viewmodel.LightLocationGroupViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import wo.lf.lifx.api.*
import wo.lf.lifx.extensions.fireAndForget
import java.util.*


class LocationGroupFragment : Fragment() {

    private val lightDetailViewModel: LightLocationGroupViewModel by viewModel { parametersOf(LocationGroupFragmentArgs.fromBundle(arguments).lightId) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_location_group, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        lightDetailViewModel.locations.observe(this, Observer { locations ->
            locations?.let { location ->
                lightDetailViewModel.light.value?.let { light ->
                    updateSpinners(light, locations)
                }
            }
        })

        lightDetailViewModel.light.observe(this, Observer { light ->
            light?.let { light ->
                lightDetailViewModel.locations.value?.let { locations ->
                    updateSpinners(light, locations)
                }
            }

            (activity as AppCompatActivity).supportActionBar?.title = lightDetailViewModel.light.value?.label
        })

        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val location = locationSpinner.adapter.getItem(position) as Location
                lightDetailViewModel.light.value?.let { light ->
                    if (location.id != light.location.id) {
                        val locationState = location.lights.first().location
                        DeviceSetLocationCommand.create(
                                light = light,
                                location = locationState.location,
                                label = locationState.label,
                                updatedAt = Date().time,
                                responseRequired = true
                        ).fireAndForget()

                        // pick first group of new location
                        val group = location.groups.first()
                        val groupState = group.lights.first().group
                        DeviceSetGroupCommand.create(
                                light = light,
                                group = groupState.group,
                                label = groupState.label,
                                updatedAt = Date().time,
                                responseRequired = true
                        ).fireAndForget()
                    }
                }
            }
        }

        groupSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val group = groupSpinner.adapter.getItem(position) as Group
                lightDetailViewModel.light.value?.let { light ->
                    if (group.id != light.group.id) {
                        val group = group.lights.first().group
                        DeviceSetGroupCommand.create(
                                light = light,
                                group = group.group,
                                label = group.label,
                                updatedAt = Date().time,
                                responseRequired = true
                        ).fireAndForget()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.let { actionBar ->
            actionBar.title = lightDetailViewModel.light.value?.label
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
        }
    }

    private fun updateSpinners(light: Light, locations: List<Location>) {
        locationSpinner.adapter = LocationSpinnerAdapter(context, locations)
        locationSpinner.setSelection(locations.indexOfFirst { it.id == light.location.id })

        val groups = locations.first { it.id == light.location.id }.groups
        groupSpinner.adapter = GroupSpinnerAdapter(context, groups)
        groupSpinner.setSelection(groups.indexOfFirst { it.id == light.group.id })
    }
}

class LocationSpinnerAdapter(context: Context?, locations: List<Location>) : ArrayAdapter<Location>(context, android.R.layout.simple_spinner_dropdown_item, locations) {
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(position, convertView, parent).apply {
            findViewById<CheckedTextView>(android.R.id.text1).text = getItem(position).name
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(position, convertView, parent).apply {
            findViewById<CheckedTextView>(android.R.id.text1).text = getItem(position).name
        }
    }
}

class GroupSpinnerAdapter(context: Context?, groups: List<Group>) : ArrayAdapter<Group>(context, android.R.layout.simple_spinner_dropdown_item, groups) {
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(position, convertView, parent).apply {
            findViewById<CheckedTextView>(android.R.id.text1).text = getItem(position).name
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(position, convertView, parent).apply {
            findViewById<CheckedTextView>(android.R.id.text1).text = getItem(position).name
        }
    }
}
