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
package org.cloudsimplus.examples

import ch.qos.logback.classic.Level
import org.cloudsimplus.allocationpolicies.VmAllocationPolicyFirstFit
import org.cloudsimplus.brokers.DatacenterBroker
import org.cloudsimplus.brokers.DatacenterBrokerSimple
import org.cloudsimplus.cloudlets.Cloudlet
import org.cloudsimplus.cloudlets.CloudletSimple
import org.cloudsimplus.core.CloudSimPlus
import org.cloudsimplus.datacenters.Datacenter
import org.cloudsimplus.datacenters.DatacenterSimple
import org.cloudsimplus.hosts.Host
import org.cloudsimplus.hosts.HostSimple
import org.cloudsimplus.resources.Pe
import org.cloudsimplus.resources.PeSimple
import org.cloudsimplus.util.Log
import org.cloudsimplus.util.TimeUtil
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic
import org.cloudsimplus.vms.Vm
import org.cloudsimplus.vms.VmSimple
import java.time.LocalDateTime

/**
 * An example creating a huge number of Hosts, VMs and Cloudlets
 * to simulate a large-scale cloud infrastructure.
 *
 *
 * The example may run out of memory.
 * Try to increase heap memory space passing, for instance,
 * -Xmx6g to the java command line, where 6g means 6GB of maximum heap size.
 *
 *
 * Your computer may not even have enough memory capacity to run this example
 * and it may just crashes with OutOfMemoryException.
 *
 *
 * Some factors that drastically impact simulation performance and memory consumption
 * is the [.CLOUDLETS] number and [.SCHEDULING_INTERVAL].
 *
 * @author Manoel Campos da Silva Filho
 * @since ClodSimPlus 7.3.1
 */
class LargeScaleExample private constructor() {
    private val simulation: CloudSimPlus
    private val broker0: DatacenterBroker
    private val vmList: List<Vm>
    private val cloudletList: List<Cloudlet>
    private val datacenter0: Datacenter
    private val startSecs: Double

    init {
        // Disable logging for performance improvements.
        Log.setLevel(Level.OFF)
        startSecs = System.currentTimeMillis() / 1000.0
        println("Creating simulation scenario at " + LocalDateTime.now())
        System.out.printf("Creating 1 Datacenter -> Hosts: %,d VMs: %,d Cloudlets: %,d%n", servers_per_site, VMS_per_user, numTasksPerUser)
        simulation = CloudSimPlus()

        datacenter0 = createDatacenter()

        //Creates a broker that is a software acting on behalf of a cloud customer to manage his/her VMs and Cloudlets
        broker0 = DatacenterBrokerSimple(simulation)
        vmList = createVms()
        cloudletList = createCloudlets()
        brokerSubmit()
        println("Starting simulation after " + actualElapsedTime())
        simulation.start()
        val submittedCloudlets = broker0.getCloudletSubmittedList().size.toLong()
        val cloudletFinishedList = broker0.getCloudletFinishedList<Cloudlet>().size.toLong()
        System.out.printf("Submitted Cloudlets: %d Finished Cloudlets: %d%n", submittedCloudlets, cloudletFinishedList)
        System.out.printf(
            "Simulated time: %s Actual Execution Time: %s%n", simulatedTime(), actualElapsedTime()
        )
    }

    private fun simulatedTime(): String {
        return TimeUtil.secondsToStr(simulation.clock())
    }

    private fun actualElapsedTime(): String {
        return TimeUtil.secondsToStr(TimeUtil.elapsedSeconds(startSecs))
    }

    private fun brokerSubmit() {
        System.out.printf("Submitting %,d VMs%n", VMS_per_user)
        broker0.submitVmList(vmList)
        System.out.printf("Submitting %,d Cloudlets%n", numTasksPerUser)
        broker0.submitCloudletList(cloudletList)
    }

    /**
     * Creates a Datacenter and its Hosts.
     */
    private fun createDatacenter(): Datacenter {
        val hostList = ArrayList<Host>()
        System.out.printf("Creating %,d Hosts%n", servers_per_site)
        for (i in 0 until servers_per_site) {
            val host = createHost()
            hostList.add(host)
        }
        val dc = DatacenterSimple(simulation, hostList, VmAllocationPolicyFirstFit())
        dc.setSchedulingInterval(SCHEDULING_INTERVAL)
        return dc
    }

    private fun createHost(): Host {
        val peList = ArrayList<Pe>(cores_per_server)
        //List of Host's CPUs (Processing Elements, PEs)
        for (i in 0 until cores_per_server) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            peList.add(PeSimple(mipsPerCoreServer.toDouble()))
        }

        /*
        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
        and VmSchedulerSpaceShared for VM scheduling.
        */return HostSimple((2048).toLong(), 10000, 1000000, peList)
    }

    /**
     * Creates a list of VMs.
     */
    private fun createVms(): List<Vm> {
        val vmList = ArrayList<Vm>(VMS_per_user)
        System.out.printf("Creating %,d VMs%n", VMS_per_user)
        for (i in 0 until VMS_per_user) {
            //Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
            val vm = VmSimple(VM_mips.toDouble(), VM_PES.toLong())
            vm.setRam(512).setBw(10000).setSize(10000)
            vmList.add(vm)
        }
        return vmList
    }

    /**
     * Creates a list of Cloudlets.
     */
    private fun createCloudlets(): List<Cloudlet> {
        val cloudletList = ArrayList<Cloudlet>(numTasksPerUser)

        //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
        val utilizationModel = UtilizationModelDynamic(0.5)
        System.out.printf("Creating %,d Cloudlets%n", numTasksPerUser)
        for (i in 0 until numTasksPerUser) {
            val cloudlet = CloudletSimple(taskLength, VMS_per_user, utilizationModel)
            cloudlet.setSizes(1024)
            cloudletList.add(cloudlet)
        }
        return cloudletList
    }

    companion object {
        private const val sites = 4
        private const val users_per_site = 1
        private const val VMS_per_user = 1
        private const val VM_PES = 4
        private const val VM_mips = 100

        private const val totalTask = 100 // 1000, 10000

        private const val servers_per_site = 1
        private const val cores_per_server = 4
        private const val mipsPerCoreServer = 1000.0
        private const val numTasksPerUser = totalTask/ sites
        private const val taskLength = 100000000L


        /**
         * Defines a time interval to process cloudlets execution
         * and possibly collect data. Setting a value greater than 0
         * enables that interval, which cause huge performance penaults for
         * lage scale simulations.
         *
         * @see Datacenter.setSchedulingInterval
         */
        private const val SCHEDULING_INTERVAL = -1.0
        @JvmStatic
        fun main(args: Array<String>) {
            LargeScaleExample()
        }
    }
}