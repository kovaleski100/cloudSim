/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2021 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.cloudsimplus.examples.dynamic

import org.cloudsimplus.brokers.DatacenterBroker
import org.cloudsimplus.brokers.DatacenterBrokerSimple
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.cloudsimplus.cloudlets.Cloudlet
import org.cloudsimplus.cloudlets.CloudletSimple
import org.cloudsimplus.core.CloudSimPlus
import org.cloudsimplus.datacenters.Datacenter
import org.cloudsimplus.datacenters.DatacenterSimple
import org.cloudsimplus.hosts.Host
import org.cloudsimplus.hosts.HostSimple
import org.cloudsimplus.listeners.CloudletVmEventInfo
import org.cloudsimplus.provisioners.ResourceProvisionerSimple
import org.cloudsimplus.resources.Pe
import org.cloudsimplus.resources.PeSimple
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared
import org.cloudsimplus.schedulers.vm.VmSchedulerSpaceShared
import org.cloudsimplus.utilizationmodels.UtilizationModelFull
import org.cloudsimplus.vms.Vm
import org.cloudsimplus.vms.VmSimple

/**
 * An example showing how to dynamically create one Cloudlet after the previous one finishes.
 * It stops creating Cloudlets when the number reaches [.CLOUDLETS].
 *
 *
 * This example uses CloudSim Plus Listener features to intercept when
 * the a Cloudlet finishes its execution to then request
 * the creation of a new Cloudlet. It uses the Java 8+ Lambda Functions features
 * to pass a listener to the mentioned Cloudlet, by means of the
 * [Cloudlet.addOnFinishListener] method.
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 2.2.0
 */
class CreateCloudletAfterLastFinishedOne private constructor() {
    private val hostList: MutableList<Host>
    private val vmList: ArrayList<Vm>
    private val cloudletList: MutableList<Cloudlet>
    private val broker: ArrayList<DatacenterBroker>
    private val datacenter: Datacenter
    private val simulation: CloudSimPlus

    /**
     * Default constructor that builds and starts the simulation.
     */
    init {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);
        println("Starting " + javaClass.simpleName)
        simulation = CloudSimPlus()
        hostList = ArrayList()
        cloudletList = ArrayList()
        datacenter = createDatacenter()
        broker = ArrayList()
        vmList = ArrayList()
        for( i in 0 until site)
        {
            broker.add(DatacenterBrokerSimple(simulation))
            vmList.addAll(createAndSubmitVms(broker[i]))
            createAndSubmitOneCloudlet(broker[i])
        }
        println("Starting " + javaClass.simpleName)
        simulation.start()
        for(i in 0 until site)
        {
            runSimulationAndPrintResults(broker[i])
            println(javaClass.simpleName + " finished!")
        }
    }

    private fun runSimulationAndPrintResults(broker: DatacenterBroker) {
        val cloudletFinishedList = broker.getCloudletFinishedList<Cloudlet>()
        CloudletsTableBuilder(cloudletFinishedList).build()
    }

    private fun createAndSubmitVms(broker: DatacenterBroker): List<Vm> {
        val newVmList = ArrayList<Vm>(VMS)
        for (i in 0 until VMS) {
            newVmList.add(createVm())
        }
        broker.submitVmList(newVmList)
        return newVmList
    }

    /**
     * Creates a VM with pre-defined configuration.
     *
     * @return the created VM
     */
    private fun createVm(): Vm {
        val mips = 100
        return VmSimple(mips.toDouble(), VM_PES_NUMBER.toLong())
            .setRam(512).setBw(1000).setSize(10000)
            .setCloudletScheduler(CloudletSchedulerTimeShared())
    }

    /**
     * Creates and submit one Cloudlet,
     * defining an Event Listener that is notified when such a Cloudlet
     * is finished in order to create another one.
     * Cloudlets stop to be created when the
     * number of Cloudlets reaches [.CLOUDLETS].
     */
    private fun createAndSubmitOneCloudlet(broker: DatacenterBroker) {
        val id = cloudletList.size
        val length: Long = lengthCloudlet //in number of Million Instructions (MI)
        val pesNumber = VM_PES_NUMBER
        val cloudlet = CloudletSimple(id.toLong(), length, pesNumber.toLong())
            .setFileSize(300)
            .setOutputSize(300)
            .setUtilizationModel(UtilizationModelFull())
        cloudletList.add(cloudlet)
        if (cloudletList.size < CLOUDLETS) {
            cloudlet.addOnFinishListener { info: CloudletVmEventInfo ->
                cloudletFinishListener(
                    info,
                    broker
                )
            }
        }
        broker.submitCloudlet(cloudlet)
    }

    private fun cloudletFinishListener(info: CloudletVmEventInfo, broker: DatacenterBroker) {
        System.out.printf(
            "\t# %.2f: Requesting creation of new Cloudlet after %s finishes executing.%n",
            info.time, info.cloudlet
        )
        createAndSubmitOneCloudlet(broker)
    }

    /**
     * Creates a Datacenter with pre-defined configuration.
     *
     * @return the created Datacenter
     */
    private fun createDatacenter(): Datacenter {
        for (i in 0 until HOSTS) {
            hostList.add(createHost(i))
        }
        return DatacenterSimple(simulation, hostList)
    }

    /**
     * Creates a host with pre-defined configuration.
     *
     * @param id The Host id
     * @return the created host
     */
    private fun createHost(id: Int): Host {
        val peList = ArrayList<Pe>()
        val mips: Long = 1000
        for (i in 0 until HOST_PES_NUMBER) {
            peList.add(PeSimple(mips.toDouble()))
        }
        val ram: Long = 2048 // host memory (Megabyte)
        val storage: Long = 1000000 // host storage (Megabyte)
        val bw: Long = 10000 //Megabits/s
        return HostSimple(ram, bw, storage, peList)
            .setRamProvisioner(ResourceProvisionerSimple())
            .setBwProvisioner(ResourceProvisionerSimple())
            .setVmScheduler(VmSchedulerSpaceShared())
    }

    companion object {
        private const val site = 4
        private const val HOSTS = 4
        private const val VMS = 1
        private const val HOST_PES_NUMBER = 4
        private const val VM_PES_NUMBER = 4
        private const val totalTask = 10000
        private const val CLOUDLETS = totalTask //VMS * VM_PES_NUMBER
        private const val lengthCloudlet = 100000L

        /**
         * Starts the example execution, calling the class constructor\
         * to build and run the simulation.
         *
         * @param args command line parameters
         */
        @JvmStatic
        fun main(args: Array<String>) {
            CreateCloudletAfterLastFinishedOne()
        }
    }
}