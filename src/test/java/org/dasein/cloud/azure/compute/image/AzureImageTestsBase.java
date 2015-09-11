package org.dasein.cloud.azure.compute.image;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureLocation;
import org.dasein.cloud.azure.AzureSSLSocketFactory;
import org.dasein.cloud.azure.AzureX509;
import org.dasein.cloud.azure.compute.AzureComputeServices;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import mockit.*;
public class AzureImageTestsBase {
	
	@Mocked
    ProviderContext providerContextMock;
    @Mocked
    Azure azureMock;
    @Mocked
    AzureComputeServices azureComputeServiceMock;
    @Mocked
    AzureVM azureVirtualMachineSupportMock;
    @Mocked
    AzureLocation azureLocationMock;
    @Mocked
    AzureSSLSocketFactory azureSSLSocketFactoryMock;
    @Mocked
    AzureX509 azureX509Mock;
    @Mocked
    Logger logger;
    
    protected final String ACCOUNT_NUMBER	= "TEST_ACCOUNT";
	protected final String REGION_ID		= "TEST_REGION";
	protected final String ENDPOINT			= "TEST_ENDPOINT";
	protected final String SERVICE_NAME		= "TEST_COMPUTE_SERVICE";
	protected final String DEPLOYMENT_NAME	= "TEST_DEPLOYMENT_NAME";
	protected final String ROLE_NAME		= "TEST_ROLE_NAME";
	
	protected final VirtualMachine virtualMachine = new VirtualMachine();
	
	@Before
	public void initializeExpectations() throws InternalException, CloudException {
		
		new NonStrictExpectations() {
            { azureMock.getContext(); result = providerContextMock; }
            { azureMock.getDataCenterServices(); result = azureLocationMock; }
            { azureMock.getComputeServices(); result = azureComputeServiceMock; }
        };
        new NonStrictExpectations() {
        	{ azureComputeServiceMock.getVirtualMachineSupport(); result = azureVirtualMachineSupportMock;}
        };
        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = ACCOUNT_NUMBER; }
            { providerContextMock.getRegionId(); result = REGION_ID; }
            { providerContextMock.getEndpoint(); result = ENDPOINT;}
        };
        new NonStrictExpectations() {
        	{ azureVirtualMachineSupportMock.getVirtualMachine(anyString); result = virtualMachine; }
        };
        
        if (name.getMethodName().startsWith("get")) {
        	
        }
	}
	
	protected final String 			TEST_IMAGE_ID	= "TEST_IMAGE_ID";
	protected final String 			TEST_VM_ID		= "TEST_VIRTUAL_MACHINE_ID";
	
	@Rule
    public final TestName name = new TestName();
	
	@Before
	public void initializeResources() {
		
		virtualMachine.setProviderVirtualMachineId(TEST_VM_ID);
		virtualMachine.addTag("serviceName", SERVICE_NAME);
		virtualMachine.addTag("deploymentName", DEPLOYMENT_NAME);
		virtualMachine.addTag("roleName", ROLE_NAME);
		
		if (name.getMethodName().startsWith("capture")) {
			virtualMachine.setCurrentState(VmState.STOPPED);
		}
	}
	
	@Before
	public void initializeMockUp() {
		final String expectedUrl = String.format("%s/%s/services/hostedservices/%s/deployments/%s/roleInstances/%s/Operations", 
        		ENDPOINT, ACCOUNT_NUMBER, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);
		if (name.getMethodName().startsWith("capture")) {
			final CloseableHttpResponse mockedHttpResponse = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null, new Header[]{});
			new MockUp<CloseableHttpClient>() {
	            @Mock(invocations = 1)
	            public CloseableHttpResponse execute(HttpUriRequest request) {
	            	assertEquals("Start method shoould do a POST", "POST", request.getMethod());
	            	assertEquals("Start method does not post to the correct url", expectedUrl, request.getURI().toString());
	            	return mockedHttpResponse;
	            }
	        };
		}
	}
	
	protected StatusLine getStatusLineMock(final int statusCode){
        return new MockUp<StatusLine>(){
            @Mock
            public int getStatusCode() {
                return statusCode;
            }
        }.getMockInstance();
    }

    protected CloseableHttpResponse getHttpResponseMock(final StatusLine statusLine, final HttpEntity httpEntity, final Header[] headers){
        return new MockUp<CloseableHttpResponse>(){
            @Mock
            public StatusLine getStatusLine() {
                return statusLine;
            }
            @Mock
            public HttpEntity getEntity() {
                return httpEntity;
            }
            @Mock
            public Header[] getAllHeaders() {
                return headers;
            }
        }.getMockInstance();
    }
    
}
