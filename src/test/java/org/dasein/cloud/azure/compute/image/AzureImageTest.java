package org.dasein.cloud.azure.compute.image;

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
import org.dasein.cloud.ResourceStatus;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.*;
import static org.junit.Assert.*;

import org.dasein.cloud.compute.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import junit.framework.Assert;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class AzureImageTest extends AzureImageTestsBase {

	/*
	 * capture image
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

	@Ignore
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
		
		final String expectedUrl = ENDPOINT + "/" + ACCOUNT_NUMBER + "/services/vmimages/" + machineImage.getProviderMachineImageId() + "?comp=media";
		final CloseableHttpResponse mockedHttpResponse = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null, new Header[]{});
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
            	assertDelete(request, expectedUrl);
            	return mockedHttpResponse;
            }
        };
		
        azureImageSupport.remove(machineImage.getProviderMachineImageId());  
	}
	
	/*
	 * remove image
	 */
	@Test
	public void removeOSImageShouldDeleteWithCorrectRequest() throws CloudException, InternalException {

		final AzureMachineImage machineImage = new AzureMachineImage();
		machineImage.setProviderMachineImageId(TEST_IMAGE_ID);
		machineImage.setAzureImageType("osimage");
		AzureImageSupport azureImageSupport = new AzureImageSupport(azureMock, machineImage);
		
		final String expectedUrl = ENDPOINT + "/" + ACCOUNT_NUMBER + "/services/images/" + machineImage.getProviderMachineImageId() + "?comp=media";
		final CloseableHttpResponse mockedHttpResponse = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null, new Header[]{});
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
            	assertDelete(request, expectedUrl);
            	return mockedHttpResponse;
            }
        };
		
        azureImageSupport.remove(machineImage.getProviderMachineImageId());  
	}

	/*
	 * list images
	 */
	@Test
	public void listImagesByFilterShouldGetWithCorrectRequest() throws CloudException, InternalException {
		
		ImageFilterOptions filter = ImageFilterOptions.getInstance(ImageClass.MACHINE).onPlatform(Platform.WINDOWS);
		
		AzureOSImage imageSupport = new AzureOSImage(azureMock);
		Iterable<MachineImage> images = imageSupport.listImages(filter);
		Iterator<MachineImage> imagesIter = images.iterator();
        MachineImage machineImage = imagesIter.next();
        assertEquals("Failed to match the name of the first image", "mcft_osimg_1", machineImage.getName());
        assertEquals("Failed to match the platform of the first image", Platform.WINDOWS, machineImage.getPlatform());
        machineImage = imagesIter.next();
        assertEquals("Failed to match the name of the second image", "vmimg_windows_1", machineImage.getName());
        assertEquals("Failed to match the platform of the second image", Platform.WINDOWS, machineImage.getPlatform());
	}
	
//	@Ignore	//TODO: check with "user" == provider owner
	@Test
	public void listImagesStatusByImageClassShouldGetWithCorrectRequest() throws CloudException, InternalException {
		
		AzureOSImage imageSupport = new AzureOSImage(azureMock);
		Iterable<ResourceStatus> images = imageSupport.listImageStatus(ImageClass.MACHINE);
		Iterator<ResourceStatus> imagesIter = images.iterator();
		int count = 0;
		while (imagesIter.hasNext()) {
			assertEquals(imagesIter.next().getResourceStatus(), MachineImageState.ACTIVE);
			count++;
		}
		assertEquals("Failed to find 4 images from the response!", 4, count);
	}
	
	@Test
	public void listImagesByImageClassShouldGetWithCorrectRequest() throws CloudException, InternalException {
        
		AzureOSImage imageSupport = new AzureOSImage(azureMock);
        Iterable<MachineImage> images = imageSupport.listImages(ImageClass.MACHINE);
		Iterator<MachineImage> imagesIter = images.iterator();
        MachineImage machineImage = imagesIter.next();
        assertEquals("Failed to match the name of the first image", "mcft_osimg_1", machineImage.getName());
        machineImage = imagesIter.next();
        assertEquals("Failed to match the name of the second image", "rhel_osimg_2", machineImage.getName());
        machineImage = imagesIter.next();
        assertEquals("Failed to match the name of the third image", "vmimg_windows_1", machineImage.getName());
        machineImage = imagesIter.next();
        assertEquals("Failed to match the name of the fourth image", "vmimg_rhel_2", machineImage.getName());
	}
	
//	@Ignore //TODO: check cannot find out the public images
	@Test
	public void listPublicImagesShouldGetWithCorrectRequest() throws CloudException, InternalException {
		
		AzureOSImage imageSupport = new AzureOSImage(azureMock);
		Iterable<MachineImage> images = imageSupport.listImages(ImageClass.MACHINE, "--public--");
		Iterator<MachineImage> imagesIter = images.iterator();
        int count = 0;
        while (imagesIter.hasNext()) {
        	assertEquals("Failed to match image owner to --public--", "--public--", imagesIter.next().getProviderOwnerId());
        	count++;
        }
        assertEquals("Failed to find images belongs to --public--", 2, count);
	}
	
	@Test
	public void listPublicMachineImagesShouldGetWithCorrectRequest() throws CloudException, InternalException {
		
		AzureOSImage imageSupport = new AzureOSImage(azureMock);
		Iterable<MachineImage> images = imageSupport.listMachineImagesOwnedBy("--public--");
		Iterator<MachineImage> imagesIter = images.iterator();
		int count = 0;
        while (imagesIter.hasNext()) {
        	assertEquals("Failed to match image owner to --public--", "--public--", imagesIter.next().getProviderOwnerId());
        	count++;
        }
        assertEquals("Failed to find images belongs to --public--", 2, count);
	}
	
}
