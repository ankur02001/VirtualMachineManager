package com.vmware.general;

/////////////////////////////////////////////////////////////////////
//RealTime.java - Helps in performance measurements of server      //
//                  and vm      	                               //
//ver 1.0                                                          //
//Language:      Java                                              //
//Platform:      Dell, Windows 8.1                                 //
//Application:   Project 1, Cloud Computing, spring2015            //
//Author:		 Ankur Pandey                                      //
// Ref: RealTime.java VMware, Inc. 2010-2012                       //
/////////////////////////////////////////////////////////////////////
import com.vmware.common.annotations.Action;
import com.vmware.connection.BasicConnection;
import com.vmware.connection.ConnectedVimServiceBase;
import com.vmware.common.annotations.Sample;
import com.vmware.vim25.*;
import com.vmware.vm.VMotion;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import javax.xml.ws.soap.SOAPFaultException;
import com.vmware.common.annotations.Option;
import com.vmware.common.ssl.TrustAllTrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.ws.BindingProvider;

public class RealTime extends ConnectedVimServiceBase {

	private ManagedObjectReference propCollectorRef;
	private ManagedObjectReference perfManager;
	private String virtualmachinename;

	private static com.vmware.vim25.VimPortType vimPort = null;
	private static com.vmware.vim25.ServiceContent serviceContent = null;

	public FileWriter myVMPerformance = null;
	public FileWriter VMPerformance = null;
	public static String HostmachineName;
	public static String Targetmachine;

	static File virtualMachinefile = null;
	public static FileWriter virtualMachineWriter = null;

	static File hostMachinefile = null;
	public static FileWriter hostMachineWriter = null;

	static File migrateMachinefile = null;
	public static FileWriter migrateMachineWriter = null;
	
	public static VMotion vMotion = null;

	public long cpuThreshold = 1450;
	public long memThreshold = 4400;
	public int MigrateCheckTime = 12;

	public RealTime(ServiceContent content, VimPortType vimport) {
		try {
			serviceContent = content;
			vimPort = vimport;
			virtualmachinename = "apandey_vm";
			HostmachineName = "128.230.96.40";
			Targetmachine = "128.230.96.111";

			// file creation
			virtualMachinefile = new File(".\\virtualMachinefile.txt");
			hostMachinefile = new File(".\\hostMachinefile.txt");
			migrateMachinefile = new File(".\\migrateMachinefile.txt");

			virtualMachinefile.createNewFile();
			hostMachinefile.createNewFile();
			migrateMachinefile.createNewFile();

			virtualMachineWriter = new FileWriter(virtualMachinefile, true);
			hostMachineWriter = new FileWriter(hostMachinefile, true);
			migrateMachineWriter = new FileWriter(migrateMachinefile, true);

			virtualMachineWriter.write("\n Output For Requirement number 1\n");
			hostMachineWriter.write("\n Output For Requirement number 2\n");
			migrateMachineWriter.write("\n Output For Requirement number 3\n");

			virtualMachineWriter
					.write("\n************************************************************************** \n");
			hostMachineWriter
					.write("\n************************************************************************** \n");
			migrateMachineWriter
					.write("\n************************************************************************** \n");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @return TraversalSpec specification to get to the HostSystem managed
	 *         object.
	 */
	TraversalSpec getHtSystemTraversalSpec() {
		// Create a traversal spec that starts from the 'root' objects
		// and traverses the inventory tree to get to the Host system.
		// Build the traversal specs bottoms up
		SelectionSpec ss = new SelectionSpec();
		ss.setName("VisitFolders");

		// Traversal to get to the host from ComputeResource
		TraversalSpec computeResourceToHostSystem = new TraversalSpec();
		computeResourceToHostSystem.setName("computeResourceToHostSystem");
		computeResourceToHostSystem.setType("ComputeResource");
		computeResourceToHostSystem.setPath("host");
		computeResourceToHostSystem.setSkip(false);
		computeResourceToHostSystem.getSelectSet().add(ss);

		// Traversal to get to the ComputeResource from hostFolder
		TraversalSpec hostFolderToComputeResource = new TraversalSpec();
		hostFolderToComputeResource.setName("hostFolderToComputeResource");
		hostFolderToComputeResource.setType("Folder");
		hostFolderToComputeResource.setPath("childEntity");
		hostFolderToComputeResource.setSkip(false);
		hostFolderToComputeResource.getSelectSet().add(ss);

		// Traversal to get to the hostFolder from DataCenter
		TraversalSpec dataCenterToHostFolder = new TraversalSpec();
		dataCenterToHostFolder.setName("DataCenterToHostFolder");
		dataCenterToHostFolder.setType("Datacenter");
		dataCenterToHostFolder.setPath("hostFolder");
		dataCenterToHostFolder.setSkip(false);
		dataCenterToHostFolder.getSelectSet().add(ss);

		// TraversalSpec to get to the DataCenter from rootFolder
		TraversalSpec traversalSpec = new TraversalSpec();
		traversalSpec.setName("VisitFolders");
		traversalSpec.setType("Folder");
		traversalSpec.setPath("childEntity");
		traversalSpec.setSkip(false);

		List<SelectionSpec> sSpecArr = new ArrayList<SelectionSpec>();
		sSpecArr.add(ss);
		sSpecArr.add(dataCenterToHostFolder);
		sSpecArr.add(hostFolderToComputeResource);
		sSpecArr.add(computeResourceToHostSystem);
		traversalSpec.getSelectSet().addAll(sSpecArr);
		return traversalSpec;
	}

	/**
	 * Retrieves the MOREF of the host.
	 *
	 * @param hostName
	 *            :
	 * @return
	 */
	ManagedObjectReference getHtByHtName(String htName)
			throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
		ManagedObjectReference retVal = null;
		ManagedObjectReference rootFolder = serviceContent.getRootFolder();
		TraversalSpec tSpec = getHtSystemTraversalSpec();
		// Create Property Spec
		PropertySpec propertySpec = new PropertySpec();
		propertySpec.setAll(Boolean.FALSE);
		propertySpec.getPathSet().add("name");
		propertySpec.setType("HostSystem");

		// Now create Object Spec
		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(rootFolder);
		objectSpec.setSkip(Boolean.TRUE);
		objectSpec.getSelectSet().add(tSpec);

		// Create PropertyFilterSpec using the PropertySpec and ObjectPec
		// created above.
		PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
		propertyFilterSpec.getPropSet().add(propertySpec);
		propertyFilterSpec.getObjectSet().add(objectSpec);
		List<PropertyFilterSpec> listpfs = new ArrayList<PropertyFilterSpec>(1);
		listpfs.add(propertyFilterSpec);
		List<ObjectContent> listobjcont = retrievePropertiesAllObjects(listpfs);

		if (listobjcont != null) {
			for (ObjectContent oc : listobjcont) {
				ManagedObjectReference mr = oc.getObj();
				String htnm = null;
				List<DynamicProperty> listDynamicProps = oc.getPropSet();
				DynamicProperty[] dps = listDynamicProps
						.toArray(new DynamicProperty[listDynamicProps.size()]);
				if (dps != null) {
					for (DynamicProperty dp : dps) {
						htnm = (String) dp.getVal();
					}
				}
				if (htnm != null && htnm.equals(htName)) {
					retVal = mr;
					break;
				}
			}
		} else {
			System.out.println("The Object Null");
		}
		if (retVal == null) {
			throw new RuntimeException("Host not found.");
		}
		return retVal;
	}

	/**
	 * Uses the new RetrievePropertiesEx method to emulate the now deprecated
	 * RetrieveProperties method.
	 *
	 * @param listpfs
	 * @return list of object content
	 * @throws Exception
	 */
	List<ObjectContent> retrievePropertiesAllObjects(
			List<PropertyFilterSpec> listpfs) {

		RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();

		List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();

		try {
			RetrieveResult rslts = vimPort.retrievePropertiesEx(
					propCollectorRef, listpfs, propObjectRetrieveOpts);
			if (rslts != null && rslts.getObjects() != null
					&& !rslts.getObjects().isEmpty()) {
				listobjcontent.addAll(rslts.getObjects());
			}
			String token = null;
			if (rslts != null && rslts.getToken() != null) {
				token = rslts.getToken();
			}
			while (token != null && !token.isEmpty()) {
				rslts = vimPort.continueRetrievePropertiesEx(propCollectorRef,
						token);
				token = null;
				if (rslts != null) {
					token = rslts.getToken();
					if (rslts.getObjects() != null
							&& !rslts.getObjects().isEmpty()) {
						listobjcontent.addAll(rslts.getObjects());
					}
				}
			}
		} catch (SOAPFaultException sfe) {
			printSoapFaultException(sfe);
		} catch (Exception e) {
			System.out.println(" : Failed Getting Contents");
			e.printStackTrace();
		}

		return listobjcontent;
	}

	/**
	 * Migrate Status Check
	 */
	public void migrateVitualMachineStatus(long htCpuUse, long htMemUse)
			throws IOException {
		if ( htCpuUse > cpuThreshold || htMemUse > memThreshold) {
			migrateMachineWriter.write("Migrating HostMachine " +HostmachineName + "to Target "+ Targetmachine + " \n ");
			System.out.println("Migrating HostMachine " +HostmachineName + "to Target "+ Targetmachine + " \n ");
			migrateMachineWriter.write("\n*****\n");
			migrateVitualMachine();
			System.out.println("\n stopping Programme As host has been moved \n");
			migrateMachineWriter.write("\n stopping Programme As host has been moved \n");
			System.out.println("\n Finished \n");
			migrateMachineWriter.flush();
            System.exit(0);    
		}
	}

	/**
	 * Migrate the virtual Machine using VMotion
	 */
	public void migrateVitualMachine() {

		BasicConnection basicConnect = new BasicConnection();
		basicConnect.setUrl("https://128.230.247.52/sdk");
		basicConnect.setUsername("AD\\apandey");
		basicConnect.setPassword("Hanu2Man!");

		vMotion = new VMotion();
		vMotion.setConnection(basicConnect.connect());
		vMotion.basicConnectionFromConnection(vMotion.connect());
		vMotion.setVmName(virtualmachinename);
		vMotion.setPriority("high_Priority");
		vMotion.setSourceHost(HostmachineName);
		vMotion.setHostConnection(true);
		vMotion.setTargetHost(Targetmachine);
		vMotion.setTargetPool("apandey");
		vMotion.setTargetDS("Storm_store1");

		try {
			vMotion.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Displaying Host Machine
	 */
	public long[] displayHtValues(List<PerfEntityMetricBase> htcpuValues,
			List<PerfEntityMetricBase> htmemValues,
			Map<Integer, PerfCounterInfo> htCPUCounters,
			Map<Integer, PerfCounterInfo> htMemoryCounters) throws IOException {
		 long[] htStatus = { 0, 0};
		for (int i = 0; i < htcpuValues.size(); ++i) {
			List<PerfMetricSeries> listpems = ((PerfEntityMetric) htcpuValues
					.get(i)).getValue();
			List<PerfSampleInfo> listinfo = ((PerfEntityMetric) htcpuValues
					.get(i)).getSampleInfo();
			hostMachineWriter.write(listinfo.get(listinfo.size() - 1)
					.getTimestamp().toString());
			hostMachineWriter.write("\n*******\n");
			long cpuCount = 0;
			for (int vi = 0; vi < listpems.size(); ++vi) {
				if (listpems.get(vi) instanceof PerfMetricIntSeries) {
					PerfMetricIntSeries val = (PerfMetricIntSeries) listpems
							.get(vi);
					List<Long> lislon = val.getValue();
					for (Long k : lislon) {
						cpuCount += k;
					}
				}
			}
			cpuCount = cpuCount / listpems.size();
			htStatus[0] = cpuCount;
			hostMachineWriter.write("host cpu " + cpuCount + "Mhz \n");
			System.out.println(" host cpu " + cpuCount + "Mhz \n");
		}

		for (int i = 0; i < htmemValues.size(); ++i) {
			List<PerfMetricSeries> listpems = ((PerfEntityMetric) htmemValues
					.get(i)).getValue();
			long memCount = 0;
			for (int vi = 0; vi < listpems.size(); ++vi) {
				if (listpems.get(vi) instanceof PerfMetricIntSeries) {
					PerfMetricIntSeries val = (PerfMetricIntSeries) listpems
							.get(vi);
					List<Long> lislon = val.getValue();
					for (Long k : lislon) {
						memCount += k;
					}
				}
			}
			memCount = (memCount / listpems.size()) / 1000;
			htStatus[1] = memCount;
			hostMachineWriter.write("host mem " + memCount + "MB\n");
			System.out.println(" host mem " + memCount + "MB\n");
		}
		return htStatus;
	}

	/**
	 * Displaying Virtual Machine
	*/
	public void displayVMValues(List<PerfEntityMetricBase> cpuValues,
			List<PerfEntityMetricBase> memValues,
			Map<Integer, PerfCounterInfo> CPUCounters,
			Map<Integer, PerfCounterInfo> MemoryCounters) throws IOException {
		for (int i = 0; i < cpuValues.size(); ++i) {
			List<PerfMetricSeries> listpems = ((PerfEntityMetric) cpuValues
					.get(i)).getValue();
			List<PerfSampleInfo> listinfo = ((PerfEntityMetric) cpuValues
					.get(i)).getSampleInfo();
			virtualMachineWriter.write("\n");
			virtualMachineWriter.write(listinfo.get(listinfo.size() - 1)
					.getTimestamp().toString());
			virtualMachineWriter.write("\n*******\n");
			long cpuCount = 0;
			for (int vi = 0; vi < listpems.size(); ++vi) {
				if (listpems.get(vi) instanceof PerfMetricIntSeries) {
					PerfMetricIntSeries val = (PerfMetricIntSeries) listpems
							.get(vi);
					List<Long> lislon = val.getValue();
					for (Long k : lislon) {
						cpuCount += k;
					}
				}
			}
			cpuCount = cpuCount / listpems.size();
			virtualMachineWriter.write(" virtual Machine cpu " + cpuCount
					+ "Mhz \n");
			System.out.println(" virtual Machine cpu " + cpuCount + "Mhz \n");
		}
		
		for (int i = 0; i < memValues.size(); ++i) {
			List<PerfMetricSeries> listpems = ((PerfEntityMetric) memValues
					.get(i)).getValue();
			long memCount = 0;
			for (int vi = 0; vi < listpems.size(); ++vi) {
				if (listpems.get(vi) instanceof PerfMetricIntSeries) {
					PerfMetricIntSeries val = (PerfMetricIntSeries) listpems
							.get(vi);
					List<Long> lislon = val.getValue();
					for (Long k : lislon) {
						memCount += k;
					}
				}
			}
			memCount = memCount / listpems.size() / 1000;
			virtualMachineWriter.write(" Virtual mem " + memCount + "MB\n");
			System.out.println(" Virtual mem " + memCount + "MB\n");
		}
	}

	/**
	 * This method initializes all the performance counters available on the
	 * system it is connected to. The performance counters are stored in the
	 * hashmap counters with group.counter.rolluptype being the key and id being
	 * the value.
	 */
	List<PerfCounterInfo> getPerfCounters() {
		List<PerfCounterInfo> pciArr = null;

		try {
			// Create Property Spec
			PropertySpec propertySpec = new PropertySpec();
			propertySpec.setAll(Boolean.FALSE);
			propertySpec.getPathSet().add("perfCounter");
			propertySpec.setType("PerformanceManager");
			List<PropertySpec> propertySpecs = new ArrayList<PropertySpec>();
			propertySpecs.add(propertySpec);

			// Now create Object Spec
			ObjectSpec objectSpec = new ObjectSpec();
			objectSpec.setObj(perfManager);
			List<ObjectSpec> objectSpecs = new ArrayList<ObjectSpec>();
			objectSpecs.add(objectSpec);

			// Create PropertyFilterSpec using the PropertySpec and ObjectPec
			// created above.
			PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
			propertyFilterSpec.getPropSet().add(propertySpec);
			propertyFilterSpec.getObjectSet().add(objectSpec);

			List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<PropertyFilterSpec>();
			propertyFilterSpecs.add(propertyFilterSpec);

			List<PropertyFilterSpec> listpfs = new ArrayList<PropertyFilterSpec>(
					1);
			listpfs.add(propertyFilterSpec);
			List<ObjectContent> listobjcont = retrievePropertiesAllObjects(listpfs);

			if (listobjcont != null) {
				for (ObjectContent oc : listobjcont) {
					List<DynamicProperty> dps = oc.getPropSet();
					if (dps != null) {
						for (DynamicProperty dp : dps) {
							List<PerfCounterInfo> pcinfolist = ((ArrayOfPerfCounterInfo) dp
									.getVal()).getPerfCounterInfo();
							pciArr = pcinfolist;
						}
					}
				}
			}
		} catch (SOAPFaultException sfe) {
			printSoapFaultException(sfe);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pciArr;
	}

	/**
	 * @return TraversalSpec specification to get to the VirtualMachine managed
	 *         object.
	 */
	TraversalSpec getVMTraversalSpec() {
		// Create a traversal spec that starts from the 'root' objects
		// and traverses the inventory tree to get to the VirtualMachines.
		// Build the traversal specs bottoms up

		// Traversal to get to the VM in a VApp
		TraversalSpec vAppToVM = new TraversalSpec();
		vAppToVM.setName("vAppToVM");
		vAppToVM.setType("VirtualApp");
		vAppToVM.setPath("vm");

		// Traversal spec for VApp to VApp
		TraversalSpec vAppToVApp = new TraversalSpec();
		vAppToVApp.setName("vAppToVApp");
		vAppToVApp.setType("VirtualApp");
		vAppToVApp.setPath("resourcePool");
		// SelectionSpec for VApp to VApp recursion
		SelectionSpec vAppRecursion = new SelectionSpec();
		vAppRecursion.setName("vAppToVApp");
		// SelectionSpec to get to a VM in the VApp
		SelectionSpec vmInVApp = new SelectionSpec();
		vmInVApp.setName("vAppToVM");
		// SelectionSpec for both VApp to VApp and VApp to VM
		List<SelectionSpec> vAppToVMSS = new ArrayList<SelectionSpec>();
		vAppToVMSS.add(vAppRecursion);
		vAppToVMSS.add(vmInVApp);
		vAppToVApp.getSelectSet().addAll(vAppToVMSS);

		// This SelectionSpec is used for recursion for Folder recursion
		SelectionSpec sSpec = new SelectionSpec();
		sSpec.setName("VisitFolders");

		// Traversal to get to the vmFolder from DataCenter
		TraversalSpec dataCenterToVMFolder = new TraversalSpec();
		dataCenterToVMFolder.setName("DataCenterToVMFolder");
		dataCenterToVMFolder.setType("Datacenter");
		dataCenterToVMFolder.setPath("vmFolder");
		dataCenterToVMFolder.setSkip(false);
		dataCenterToVMFolder.getSelectSet().add(sSpec);

		// TraversalSpec to get to the DataCenter from rootFolder
		TraversalSpec traversalSpec = new TraversalSpec();
		traversalSpec.setName("VisitFolders");
		traversalSpec.setType("Folder");
		traversalSpec.setPath("childEntity");
		traversalSpec.setSkip(false);
		List<SelectionSpec> sSpecArr = new ArrayList<SelectionSpec>();
		sSpecArr.add(sSpec);
		sSpecArr.add(dataCenterToVMFolder);
		sSpecArr.add(vAppToVM);
		sSpecArr.add(vAppToVApp);
		traversalSpec.getSelectSet().addAll(sSpecArr);
		return traversalSpec;
	}

	/**
	 * Get the MOR of the Virtual Machine by its name.
	 *
	 * @param vmName
	 *            The name of the Virtual Machine
	 * @return The Managed Object reference for this VM
	 */
	ManagedObjectReference getVmByVMname(String vmName) {
		ManagedObjectReference retVal = null;
		ManagedObjectReference rootFolder = serviceContent.getRootFolder();

		try {
			TraversalSpec tSpec = getVMTraversalSpec();
			// Create Property Spec
			PropertySpec propertySpec = new PropertySpec();
			propertySpec.setAll(Boolean.FALSE);
			propertySpec.getPathSet().add("name");
			propertySpec.setType("VirtualMachine");

			// Now create Object Spec
			ObjectSpec objectSpec = new ObjectSpec();
			objectSpec.setObj(rootFolder);
			objectSpec.setSkip(Boolean.TRUE);
			objectSpec.getSelectSet().add(tSpec);

			// Create PropertyFilterSpec using the PropertySpec and ObjectPec
			// created above.
			PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
			propertyFilterSpec.getPropSet().add(propertySpec);
			propertyFilterSpec.getObjectSet().add(objectSpec);

			List<PropertyFilterSpec> listpfs = new ArrayList<PropertyFilterSpec>(
					1);
			listpfs.add(propertyFilterSpec);
			List<ObjectContent> listobjcont = retrievePropertiesAllObjects(listpfs);

			if (listobjcont != null) {
				for (ObjectContent oc : listobjcont) {
					ManagedObjectReference mr = oc.getObj();
					String vmnm = null;
					List<DynamicProperty> dps = oc.getPropSet();

					if (dps != null) {
						for (DynamicProperty dp : dps) {
							vmnm = (String) dp.getVal();
						}
					}
					if (vmnm != null && vmnm.equals(vmName)) {
						retVal = mr;
						break;
					}
				}
			}
		} catch (SOAPFaultException sfe) {
			printSoapFaultException(sfe);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retVal;
	}

	/**
	 * @param pmRef
	 * @param vmRef
	 * @param htRef
	 * @param mMetrics
	 * @param counters
	 * @throws IOException
	 * @throws Exception
	 */
	void monitorPerformance(ManagedObjectReference pmRef,
			ManagedObjectReference vmmor, List<PerfMetricId> mCPUMetrics,
			List<PerfMetricId> mMemMetrics,
			Map<Integer, PerfCounterInfo> CPUcounters,
			Map<Integer, PerfCounterInfo> Memorycounters,
			ManagedObjectReference htRef, List<PerfMetricId> htCPUMetrics,
			List<PerfMetricId> htMemMetrics,
			Map<Integer, PerfCounterInfo> htCPUcounters,
			Map<Integer, PerfCounterInfo> htMemorycounters)
			throws RuntimeFaultFaultMsg, InterruptedException, IOException {
		// virtual Machine
		PerfQuerySpec qSpecCPU = new PerfQuerySpec();
		qSpecCPU.setEntity(vmmor);
		qSpecCPU.setMaxSample(new Integer(10));
		qSpecCPU.getMetricId().addAll(mCPUMetrics);
		qSpecCPU.setIntervalId(new Integer(20));
		PerfQuerySpec qSpecMem = new PerfQuerySpec();
		qSpecMem.setEntity(vmmor);
		qSpecMem.setMaxSample(new Integer(10));
		qSpecMem.getMetricId().addAll(mMemMetrics);
		qSpecMem.setIntervalId(new Integer(20));

		// Host Machine
		PerfQuerySpec htqSpecCPU = new PerfQuerySpec();
		htqSpecCPU.setEntity(htRef);
		htqSpecCPU.setMaxSample(new Integer(10));
		htqSpecCPU.getMetricId().addAll(htCPUMetrics);
		htqSpecCPU.setIntervalId(new Integer(20));
		PerfQuerySpec htqSpecMem = new PerfQuerySpec();
		htqSpecMem.setEntity(htRef);
		htqSpecMem.setMaxSample(new Integer(10));
		htqSpecMem.getMetricId().addAll(htMemMetrics);
		htqSpecMem.setIntervalId(new Integer(20));

		// Virtual Machine
		List<PerfQuerySpec> qCPUSpecs = new ArrayList<PerfQuerySpec>();
		qCPUSpecs.add(qSpecCPU);
		List<PerfQuerySpec> qMemSpecs = new ArrayList<PerfQuerySpec>();
		qMemSpecs.add(qSpecMem);

		// Host Machine
		List<PerfQuerySpec> htqCPUSpecs = new ArrayList<PerfQuerySpec>();
		htqCPUSpecs.add(htqSpecCPU);
		List<PerfQuerySpec> htqMemSpecs = new ArrayList<PerfQuerySpec>();
		htqMemSpecs.add(htqSpecMem);

		int counter = 0;
		long[] htStatus= {0,0};
		while (true) {
			// virtual Machine
			List<PerfEntityMetricBase> cpulistpemb = vimPort.queryPerf(pmRef,
					qCPUSpecs);
			List<PerfEntityMetricBase> pValuesCPU = cpulistpemb;
			List<PerfEntityMetricBase> memlistpemb = vimPort.queryPerf(pmRef,
					qMemSpecs);
			List<PerfEntityMetricBase> pValuesMem = memlistpemb;

			// Host Machine
			List<PerfEntityMetricBase> htcpulistpemb = vimPort.queryPerf(pmRef,
					htqCPUSpecs);
			List<PerfEntityMetricBase> htpValuesCPU = htcpulistpemb;
			List<PerfEntityMetricBase> htmemlistpemb = vimPort.queryPerf(pmRef,
					htqMemSpecs);
			List<PerfEntityMetricBase> htpValuesMem = htmemlistpemb;

			if (pValuesCPU != null && pValuesMem != null
					&& htpValuesCPU != null && htpValuesMem != null) {
				// Display virtual Machine Values
				displayVMValues(pValuesCPU, pValuesMem, CPUcounters,
						Memorycounters);
				// Display Host Values
				htStatus = displayHtValues(htpValuesCPU, htpValuesMem, htCPUcounters,
						htMemorycounters);
			}
			counter++;
			if (counter == MigrateCheckTime) {
				counter = 0;
				migrateVitualMachineStatus(htStatus[0], htStatus[1]);
			}
			virtualMachineWriter.write("\nSleeping 15 seconds \n");
			hostMachineWriter.write("\nSleeping 15 seconds \n");
			System.out.println("\nSleeping 15 seconds \n");
			virtualMachineWriter.flush();
			hostMachineWriter.flush();
			migrateMachineWriter.flush();
			Thread.sleep(15 * 1000);
		}
	}

	
	void printSoapFaultException(SOAPFaultException sfe) {
		System.out.println("SOAP Fault -");
		if (sfe.getFault().hasDetail()) {
			System.out.println(sfe.getFault().getDetail().getFirstChild()
					.getLocalName());
		}
		if (sfe.getFault().getFaultString() != null) {
			System.out
					.println("\n Message: " + sfe.getFault().getFaultString());
		}
	}

	void doRealTime() throws IOException, RuntimeFaultFaultMsg,
			InterruptedException, InvalidPropertyFaultMsg {

		ManagedObjectReference vmmor = getVmByVMname(virtualmachinename);
		ManagedObjectReference HtRef = getHtByHtName(HostmachineName);

		if (vmmor != null && HtRef != null) {
			// Virtual Machine settings
			List<PerfCounterInfo> cInfo = getPerfCounters();
			List<PerfCounterInfo> CpuCounters = new ArrayList<PerfCounterInfo>();
			List<PerfCounterInfo> MemCounters = new ArrayList<PerfCounterInfo>();
			for (int i = 0; i < cInfo.size(); ++i) {
				if ("cpu"
						.equalsIgnoreCase(cInfo.get(i).getGroupInfo().getKey()))
					CpuCounters.add(cInfo.get(i));
				if ("mem"
						.equalsIgnoreCase(cInfo.get(i).getGroupInfo().getKey()))
					MemCounters.add(cInfo.get(i));
			}
			Map<Integer, PerfCounterInfo> Cpucounters = new HashMap<Integer, PerfCounterInfo>();
			PerfCounterInfo CpuInfo = CpuCounters.get(5);
			Map<Integer, PerfCounterInfo> Memcounters = new HashMap<Integer, PerfCounterInfo>();
			PerfCounterInfo MemInfo = MemCounters.get(77);

			// Host Machine Settings
			List<PerfCounterInfo> htCpuCounters = new ArrayList<PerfCounterInfo>();
			List<PerfCounterInfo> htMemCounters = new ArrayList<PerfCounterInfo>();
			for (int i = 0; i < cInfo.size(); ++i) {
				if ("cpu"
						.equalsIgnoreCase(cInfo.get(i).getGroupInfo().getKey()))
					htCpuCounters.add(cInfo.get(i));
				if ("mem"
						.equalsIgnoreCase(cInfo.get(i).getGroupInfo().getKey()))
					htMemCounters.add(cInfo.get(i));
			}
			Map<Integer, PerfCounterInfo> htCpucounters = new HashMap<Integer, PerfCounterInfo>();
			PerfCounterInfo htCpuInfo = htCpuCounters.get(5);
			Map<Integer, PerfCounterInfo> htMemcounters = new HashMap<Integer, PerfCounterInfo>();
			PerfCounterInfo htMemInfo = htMemCounters.get(77);

			Cpucounters.put(new Integer(CpuInfo.getKey()), CpuInfo);
			Memcounters.put(new Integer(MemInfo.getKey()), MemInfo);

			htCpucounters.put(new Integer(htCpuInfo.getKey()), htCpuInfo);
			htMemcounters.put(new Integer(htMemInfo.getKey()), htMemInfo);

			List<PerfMetricId> listpermeid = vimPort.queryAvailablePerfMetric(
					perfManager, vmmor, null, null, new Integer(20));

			List<PerfMetricId> mCPUMetrics = new ArrayList<PerfMetricId>();
			List<PerfMetricId> mMemMetrics = new ArrayList<PerfMetricId>();
			if (listpermeid != null) {
				for (int index = 0; index < listpermeid.size(); ++index) {
					if (Cpucounters.containsKey(new Integer(listpermeid.get(
							index).getCounterId()))) {
						mCPUMetrics.add(listpermeid.get(index));
					}
					if (Memcounters.containsKey(new Integer(listpermeid.get(
							index).getCounterId()))) {
						mMemMetrics.add(listpermeid.get(index));
					}
				}
			}

			List<PerfMetricId> htpermeid = vimPort.queryAvailablePerfMetric(
					perfManager, HtRef, null, null, new Integer(20));

			List<PerfMetricId> htCPUMetrics = new ArrayList<PerfMetricId>();
			List<PerfMetricId> htMemMetrics = new ArrayList<PerfMetricId>();
			if (htpermeid != null) {
				for (int index = 0; index < htpermeid.size(); ++index) {
					if (htCpucounters.containsKey(new Integer(htpermeid.get(
							index).getCounterId()))) {
						htCPUMetrics.add(htpermeid.get(index));
					}
					if (htMemcounters.containsKey(new Integer(htpermeid.get(
							index).getCounterId()))) {
						htMemMetrics.add(htpermeid.get(index));
					}
				}
			}
			// Calling monitorPerformance with all parameters
			monitorPerformance(perfManager, vmmor, mCPUMetrics, mMemMetrics,
					Cpucounters, Memcounters, HtRef, htCPUMetrics,
					htMemMetrics, htCpucounters, htMemcounters);
		} else {
			if (vmmor == null) {
				virtualMachineWriter.write("Virtual Machine not found\n");
			}
			if (HtRef == null) {
				hostMachineWriter.write("Host Machine not found\n");
			}
		}
	}

	@Action
	public void run() throws RuntimeFaultFaultMsg, IOException,
			InterruptedException, InvalidPropertyFaultMsg {
		propCollectorRef = serviceContent.getPropertyCollector();
		perfManager = serviceContent.getPerfManager();
		doRealTime();
		virtualMachineWriter.close();
		hostMachineWriter.close();
		migrateMachineWriter.close();

	}

}
