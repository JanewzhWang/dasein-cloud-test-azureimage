package org.dasein.cloud.azure.compute.image;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.*;
import org.junit.Ignore;
import org.junit.Test;
import junit.framework.Assert;
import mockit.Expectations;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

public class AzureImageTest extends AzureImageTestsBase {

	/*
	 * capture
	 */
	@Test
	public void captureWithOptionsShouldPostCorrectRequest() throws InternalException, CloudException {

		MachineImage machineImage = MachineImage.getInstance(ACCOUNT_NUMBER, REGION_ID, TEST_IMAGE_ID,
				ImageClass.MACHINE, MachineImageState.PENDING, TEST_IMAGE_ID, TEST_IMAGE_ID, Architecture.I64,
				Platform.RHEL);
		AzureImageSupport azureImageSupport = new AzureImageSupport(azureMock, machineImage);
		MachineImage resultImage = azureImageSupport.capture(
				ImageCreateOptions.getInstance(virtualMachine, "TEST_MACHINE_IMAGE", "MACHINE IMAGE FOR TEST"), 
				null);

		Assert.assertNotNull("Machine Image is null", resultImage);
		Assert.assertEquals("Image ID is wrong", machineImage.getProviderMachineImageId(),
				resultImage.getProviderMachineImageId());
	}

	@Test
	public void captureWithTaskShouldPostCorrectRequest() throws InternalException, CloudException {

		final String IMAGE_SOFTWARE = "TEST_SOFTWARE";

		AsynchronousTask<MachineImage> task = new AsynchronousTask<MachineImage>() {
			@Override
			public synchronized void completeWithResult(@Nullable MachineImage result) {
				super.completeWithResult(result);
				result.withSoftware(IMAGE_SOFTWARE);
				result.sharedWithPublic();
			}
		};

		MachineImage machineImage = MachineImage.getInstance(ACCOUNT_NUMBER, REGION_ID, TEST_IMAGE_ID,
				ImageClass.MACHINE, MachineImageState.PENDING, TEST_IMAGE_ID, TEST_IMAGE_ID, Architecture.I64,
				Platform.RHEL);
		AzureImageSupport azureImageSupport = new AzureImageSupport(azureMock, machineImage);
		MachineImage resultImage = azureImageSupport.capture(
				ImageCreateOptions.getInstance(virtualMachine, "TEST_MACHINE_IMAGE", "MACHINE IMAGE FOR TEST"), 
				task);

		Assert.assertNotNull("Machine Image is null", resultImage);
		Assert.assertEquals("Image ID is wrong", machineImage.getProviderMachineImageId(),
				resultImage.getProviderMachineImageId());
		Assert.assertTrue("Image change public share failed", resultImage.isPublic());
		Assert.assertEquals("Image change software failed", IMAGE_SOFTWARE, resultImage.getSoftware());
	}

	@Test(expected = CloudException.class)
	public void captureShouldThrowExceptionIfRetrieveImageTimeout() throws InternalException, CloudException {
		AzureImageSupport azureImageSupport = new AzureImageSupport(azureMock);
		azureImageSupport.capture(
				ImageCreateOptions.getInstance(virtualMachine, "TEST_MACHINE_IMAGE", "MACHINE IMAGE FOR TEST"), 
				null);
	}

	@Test(expected = CloudException.class)
	public void captureShouldThrowExceptionIfTerminateVMServiceFailed() throws CloudException, InternalException {

		new Expectations() {
			{
				azureVirtualMachineSupportMock.terminateService(anyString, anyString);
				result = new CloudException("Terminate service failed!");
			}
		};

		MachineImage machineImage = MachineImage.getInstance(ACCOUNT_NUMBER, REGION_ID, TEST_IMAGE_ID,
				ImageClass.MACHINE, MachineImageState.PENDING, TEST_IMAGE_ID, TEST_IMAGE_ID, Architecture.I64,
				Platform.RHEL);
		AzureImageSupport azureImageSupport = new AzureImageSupport(azureMock, machineImage);
		azureImageSupport.capture(
				ImageCreateOptions.getInstance(virtualMachine, "TEST_MACHINE_IMAGE", "MACHINE IMAGE FOR TEST"), 
				null);
	}
	
	@Test
	public void removeMachineImageShouldDeleteWithCorrectRequest() throws CloudException, InternalException {

		final AzureMachineImage machineImage = new AzureMachineImage();
		machineImage.setProviderMachineImageId(TEST_IMAGE_ID);
		machineImage.setAzureImageType(ImageClass.MACHINE.name());
		AzureImageSupport azureImageSupport = new AzureImageSupport(azureMock, machineImage);
		
		final CloseableHttpResponse mockedHttpResponse = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null, new Header[]{});
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
            	assertEquals(request.getMethod(), "DELETE");
            	assertEquals(request.getURI().toString(), ENDPOINT + "/" + ACCOUNT_NUMBER + "/services/vmimages/" + 
            			machineImage.getProviderMachineImageId() + "?comp=media");
            	return mockedHttpResponse;
            }
        };
		
        azureImageSupport.remove(machineImage.getProviderMachineImageId());  
	}
	
	@Test
	public void removeOSImageShouldDeleteWithCorrectRequest() throws CloudException, InternalException {

		final AzureMachineImage machineImage = new AzureMachineImage();
		machineImage.setProviderMachineImageId(TEST_IMAGE_ID);
		machineImage.setAzureImageType("osimage");
		AzureImageSupport azureImageSupport = new AzureImageSupport(azureMock, machineImage);
		
		final CloseableHttpResponse mockedHttpResponse = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null, new Header[]{});
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
            	assertEquals(request.getMethod(), "DELETE");
            	assertEquals(request.getURI().toString(), ENDPOINT + "/" + ACCOUNT_NUMBER + "/services/images/" + 
            			machineImage.getProviderMachineImageId() + "?comp=media");
            	return mockedHttpResponse;
            }
        };
		
        azureImageSupport.remove(machineImage.getProviderMachineImageId());  
	}
	
	/*
	 * getImage - TODO at last
	 * listImageStatus				- getAllImages
	 * listImages 3					- getAllImages
	 * listMachineImages
	 * listMachineImagesOwnedBy
	 * searchPublicMachineImages
	 * searchPublicImages 
	 */
	@Test
	public void listImagesByImageClassShouldGetWithCorrectRequest() throws CloudException, InternalException {
		
		final String expectedOSImagesUrl = REQUEST_PREFIX + "/services/images";
		final String expectedVMImagesUrl = REQUEST_PREFIX + String.format("/services/vmimages?location=%s&category=user", REGION_ID);
		final CloseableHttpResponse mockedHttpRespWithOsImages = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), 
				getHttpEntityMock("org/dasein/cloud/azure/compute/image/osimagesmodel.xml") , new Header[]{});
		final CloseableHttpResponse mockedHttpRespWithVmImages = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), 
				getHttpEntityMock("org/dasein/cloud/azure/compute/image/vmimagesmodel.xml") , new Header[]{});
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 2)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
            		assertEquals(request.getMethod(), "GET");
            		assertEquals(request.getURI().toString(), expectedOSImagesUrl);
            		return mockedHttpRespWithOsImages;
            	} else if (inv.getInvocationCount() == 2) {
            		assertEquals(request.getMethod(), "GET");
            		assertEquals(request.getURI().toString(), expectedVMImagesUrl);
            		return mockedHttpRespWithVmImages;
            	} else {
            		throw new RuntimeException("Assert failed for more than 2 invocation found!");
            	}
            }
        };
        
        AzureImageSupport azureImageSupport = new AzureImageSupport(azureMock, null);
        Iterable<MachineImage> images = azureImageSupport.listImages(ImageClass.MACHINE);
		Iterator<MachineImage> imagesIter = images.iterator();
        MachineImage machineImage = imagesIter.next();
        assertEquals("First OS Image name is wrong", machineImage.getName(), "mcft_osimg_1");
        machineImage = imagesIter.next();
        assertEquals("Second OS Image name is wrong", machineImage.getName(), "rhel_osimg_2");
        machineImage = imagesIter.next();
        assertEquals("Third VM Image name is wrong", machineImage.getName(), "vm_img_1");
        machineImage = imagesIter.next();
        assertEquals("Fourth VM Image name is wrong", machineImage.getName(), "vm_img_2");
	}
	
	@Test
	public void listImagesByFilterShouldGetWithCorrectRequest() throws CloudException, InternalException {
		//TODO
	}
	
}
