/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.saalfeldlab.n5.bdv;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import bdv.export.ProposeMipmaps;
import bdv.tools.transformation.TransformedSource;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.util.VolatileRandomAccessibleIntervalMipmapSource;
import bdv.util.volatiles.SharedQueue;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Util;

public class N5MultiscaleSource
{
	/**
	 * Creates a volatile multiscale {@link Source} for specified channel of an N5 dataset.
	 */
	public static < T extends NumericType< T > & NativeType< T >, V extends Volatile< T > & NumericType< V > > Source< V > getVolatileSource(
			final N5Reader n5,
			final int channel,
			final String name,
			final SharedQueue queue ) throws IOException
	{
		final N5ExportMetadataReader metadata = N5ExportMetadata.openForReading( n5 );
		final RandomAccessibleIntervalMipmapSource< T > source = getRandomAccessibleIntervalMipmapSource( n5, metadata, channel, name );
		final VolatileRandomAccessibleIntervalMipmapSource< T, V > volatileSource = source.asVolatile( queue );
		final Source< V > transformedVolatileSource = applyTransform( volatileSource, metadata, channel );
		return transformedVolatileSource;
	}

	/**
	 * Creates a multiscale {@link Source} for specified channel of an N5 dataset.
	 */
	public static < T extends NumericType< T > & NativeType< T > > Source< T > getSource(
			final N5Reader n5,
			final int channel,
			final String name ) throws IOException
	{
		final N5ExportMetadataReader metadata = N5ExportMetadata.openForReading( n5 );
		final RandomAccessibleIntervalMipmapSource< T > source = getRandomAccessibleIntervalMipmapSource( n5, metadata, channel, name );
		final Source< T > transformedSource = applyTransform( source, metadata, channel );
		return transformedSource;
	}

	@SuppressWarnings("unchecked")
	private static < T extends NumericType< T > & NativeType< T > > RandomAccessibleIntervalMipmapSource< T > getRandomAccessibleIntervalMipmapSource(
			final N5Reader n5,
			final N5ExportMetadataReader metadata,
			final int channel,
			final String name ) throws IOException
	{
		final double[][] scales = metadata.getScales( channel );
		final RandomAccessibleInterval< T >[] scaleLevelImgs = new RandomAccessibleInterval[ scales.length ];
		for ( int s = 0; s < scales.length; ++s )
			scaleLevelImgs[ s ] = N5Utils.openVolatile( n5, N5ExportMetadata.getScaleLevelDatasetPath( channel, s ) );

		final RandomAccessibleIntervalMipmapSource< T > source = new RandomAccessibleIntervalMipmapSource<>(
				scaleLevelImgs,
				Util.getTypeFromInterval( scaleLevelImgs[ 0 ] ),
				scales,
				metadata.getPixelResolution( channel ),
				name );

		return source;
	}

	private static < T > Source< T > applyTransform(
			final Source< T > source,
			final N5ExportMetadataReader metadata,
			final int channel ) throws IOException
	{
		final TransformedSource< T > transformedSource = new TransformedSource<>( source );

		// account for the pixel resolution
		final VoxelDimensions voxelDimensions = source.getVoxelDimensions();
		final double[] normalizedVoxelSize = new double[] { 1, 1, 1 };
		if ( voxelDimensions != null )
		{
			voxelDimensions.dimensions( normalizedVoxelSize );
			ProposeMipmaps.normalizeVoxelSize( normalizedVoxelSize );
		}
		final AffineTransform3D voxelSizeTransform = new AffineTransform3D();
		voxelSizeTransform.preConcatenate( new Scale3D( normalizedVoxelSize ) );
		transformedSource.setFixedTransform( voxelSizeTransform );

		// prepend with the source transform
		final AffineTransform3D metadataTransform = metadata.getAffineTransform( channel );
		if ( metadataTransform != null )
			transformedSource.setIncrementalTransform( metadataTransform );

		return transformedSource;
	}
}
