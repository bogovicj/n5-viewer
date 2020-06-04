package org.janelia.saalfeldlab.n5.bdv.dataaccess.s3;

import org.janelia.saalfeldlab.n5.bdv.dataaccess.DataAccessException;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import ij.IJ;

public class AmazonS3ClientBuilderWithDefaultCredentials
{
	private static final String credentialsDocsLink = "https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#cli-quick-configuration";

	public static AmazonS3 create() throws DataAccessException
	{
		try
		{
			return AmazonS3ClientBuilder.standard().withCredentials( new ProfileCredentialsProvider() ).build();
		}
		catch ( final Exception e )
		{
			IJ.error(
					"N5 Viewer",
					"<html>Could not find AWS credentials/region. Please initialize them using AWS Command Line Interface:<br/>"
							+ "<a href=\"" + credentialsDocsLink + "\">" + credentialsDocsLink + "</a></html>"
				);
			throw new DataAccessException();
		}
	}
}
