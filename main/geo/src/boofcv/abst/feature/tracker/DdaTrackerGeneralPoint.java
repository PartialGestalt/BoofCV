/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.abst.feature.tracker;

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.alg.feature.UtilFeature;
import boofcv.alg.feature.detect.interest.EasyGeneralFeatureDetector;
import boofcv.struct.FastQueue;
import boofcv.struct.QueueCorner;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;

/**
 * Detects simple features (corners and blobs) whose location if fully described by a pixel coordinate.  Unlike more
 * generalized implementations, previously detected features can be excluded automatically when detecting new
 * features.
 *
 * @author Peter Abeles
 */
public class DdaTrackerGeneralPoint<I extends ImageSingleBand, D extends ImageSingleBand, Desc extends TupleDesc>
		extends DetectAssociateBase<I, Desc> {

	// feature detector
	private EasyGeneralFeatureDetector<I,D> detector;
	// feature descriptor
	private DescribeRegionPoint<I, Desc> describe;
	// scale that features should be created at
	private double scale;

	// storage for descriptors
	private FastQueue<Desc> descriptors;
	private FastQueue<Point2D_F64> locations = new FastQueue<Point2D_F64>(100,Point2D_F64.class,true);

	public DdaTrackerGeneralPoint( final AssociateDescription2D<Desc> associate ,
								   final boolean updateDescription,
								   EasyGeneralFeatureDetector<I,D> detector ,
								   DescribeRegionPoint<I, Desc> describe ,
								   double scale ) {
		super(associate, updateDescription, describe.getDescriptionType());
		this.detector = detector;
		this.describe = describe;
		this.scale = scale;

		descriptors = UtilFeature.createQueue(describe,100);
	}

	@Override
	protected void detectFeatures(I input, FastQueue<Point2D_F64> locDst, FastQueue<Desc> featDst) {

		// detect features in the image
		detector.detect(input,null);
		describe.setImage(input);

		QueueCorner found = detector.getMaximums();

		// compute descriptors and populate results list
		descriptors.reset();
		locations.reset();
		for( int i = 0; i < found.size; i++ ) {
			Point2D_I16 p = found.get(i);
			if( describe.isInBounds(p.x,p.y,0,scale) ) {
				Point2D_F64 loc = locations.grow();
				Desc desc = descriptors.grow();
				loc.set(p.x,p.y);
				describe.process(loc.x,loc.y,0,scale,desc);
				featDst.add(desc);
				locDst.add( loc );
			}
		}
	}

	@Override
	public Desc createDescription() {
		return describe.createDescription();
	}

	@Override
	public int getDescriptionLength() {
		return describe.getDescriptionLength();
	}

	@Override
	public Class<Desc> getDescriptionType() {
		return describe.getDescriptionType();
	}
}