package org.dasein.cloud.azure.compute.image;

import javax.annotation.Nonnull;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.compute.MachineImage;

public class AzureImageSupport extends AzureOSImage {
	
	private MachineImage machineImage;

	public AzureImageSupport(Azure provider) {
		super(provider);
	}
	
	public AzureImageSupport(Azure provider, MachineImage machineImage) {
		super(provider);
		this.machineImage = machineImage;
	}
	
	public void setMachineImage(MachineImage machineImage) {
		this.machineImage = machineImage;
	}
	
	@Override public MachineImage getImage(@Nonnull String machineImageId) throws CloudException, InternalException {
		return machineImage;
	}

}
