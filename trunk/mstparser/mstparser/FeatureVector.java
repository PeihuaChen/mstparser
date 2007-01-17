///////////////////////////////////////////////////////////////////////////////
// Copyright (C) 2007 University of Texas at Austin and (C) 2005
// University of Pennsylvania and Copyright (C) 2002, 2003 University
// of Massachusetts Amherst, Department of Computer Science.
//
// This software is licensed under the terms of the Common Public
// License, Version 1.0 or (at your option) any subsequent version.
// 
// The license is approved by the Open Source Initiative, and is
// available from their website at http://www.opensource.org.
///////////////////////////////////////////////////////////////////////////////

package mstparser;

import gnu.trove.*;
import java.util.*;



/**
 * A <tt>FeatureVector</tt> that can hold up to two
 * <tt>FeatureVector</tt> instances inside it, which allows for a very
 * quick concatenation operation.
 *
 * <p>Also, in order to avoid copies, the second of these internal
 * <tt>FeatureVector</tt> instances can be negated, so that it has the
 * effect of subtracting any values rather than adding them.
 *
 * <p>
 * Created: Sat Nov 10 15:25:10 2001
 * </p>
 *
 * @author Jason Baldridge
 * @version $Id$
 * @see mstparser.Feature
 */
public class FeatureVector extends TLinkedList {
    private FeatureVector subfv1 = null;
    private FeatureVector subfv2 = null;
    private boolean negateSecondSubFV = false;

    public FeatureVector () {}

    public FeatureVector (FeatureVector fv1) {
	subfv1 = fv1;
    }

    public FeatureVector (FeatureVector fv1, FeatureVector fv2) {
	subfv1 = fv1;
	subfv2 = fv2;
    }

    public FeatureVector (FeatureVector fv1, FeatureVector fv2, boolean negSecond) {
	subfv1 = fv1;
	subfv2 = fv2;
	negateSecondSubFV = negSecond;
    }

    public FeatureVector (int[] keys) {
	for (int i=0; i<keys.length; i++)
	    add(new Feature(keys[i],1.0));
    }

    public void add(int index, double value) {
	add(new Feature(index, value));
    }


    public int[] keys() {
	TIntArrayList keys = new TIntArrayList();
	addKeysToList(keys);
	return keys.toNativeArray();
    }

    private void addKeysToList(TIntArrayList keys) {
	if (null != subfv1) {
	    subfv1.addKeysToList(keys);

	    if (null != subfv2)
		subfv2.addKeysToList(keys);
	}

	ListIterator it = listIterator();
	while (it.hasNext())
	    keys.add(((Feature)it.next()).index);

    }


    public FeatureVector cat(FeatureVector fl2) {
	return new FeatureVector(this, fl2);
    }

    // fv1 - fv2
    public FeatureVector getDistVector(FeatureVector fl2) {
	//return new FeatureVector(this, fl2, true);
	FeatureVector nfv = new FeatureVector(this);
	fl2.addNegativeFeatures(nfv);
	return nfv;
    }

    private void addNegativeFeatures (FeatureVector fv) {
	if (null != subfv1) {
	    subfv1.addNegativeFeatures(fv);

	    if (null != subfv2)
		subfv2.addNegativeFeatures(fv);
	}

	ListIterator it = listIterator();
	while (it.hasNext())
	    fv.add(((Feature)it.next()).negation());
    }

    public double getScore(double[] parameters) {
	double score = 0.0;

	if (null != subfv1) {
	    score += subfv1.getScore(parameters);

	    if (null != subfv2) {
		double fv2score = subfv2.getScore(parameters);
		if (negateSecondSubFV)
		    score -= fv2score;
		else
		    score += fv2score;
	    }
	}

	ListIterator it = listIterator();
	while (it.hasNext()) {
	    Feature f = (Feature)it.next();
            score += parameters[f.index]*f.value;
	}

	return score;
    }

    public void update(double[] parameters, double[] total, double alpha_k, double upd) {
	update(parameters, total, alpha_k, upd, false);
    }

    private final void update(double[] parameters, double[] total, 
			      double alpha_k, double upd, boolean negate) {

	if (null != subfv1) {
	    subfv1.update(parameters, total, alpha_k, upd, negate);

	    if (null != subfv2)
		subfv2.update(parameters, total, alpha_k, upd, negateSecondSubFV);
	}


	ListIterator it = listIterator();

	if (negate) {
	    while (it.hasNext()) {
		Feature f = (Feature)it.next();
		parameters[f.index] -= alpha_k*f.value;
		total[f.index] -= upd*alpha_k*f.value;
	    }
	} else {
	    while (it.hasNext()) {
		Feature f = (Feature)it.next();
		parameters[f.index] += alpha_k*f.value;
		total[f.index] += upd*alpha_k*f.value;
	    }
	}

    }

	
    public double dotProduct(FeatureVector fl2) {

	TIntDoubleHashMap hm1 = new TIntDoubleHashMap(this.size());
	addFeaturesToMap(hm1, false);
	hm1.compact();

	TIntDoubleHashMap hm2 = new TIntDoubleHashMap(fl2.size());
	fl2.addFeaturesToMap(hm2, negateSecondSubFV);
	hm2.compact();

	int[] keys = hm1.keys();

	double result = 0.0;
	for(int i = 0; i < keys.length; i++)
	    result += hm1.get(keys[i])*hm2.get(keys[i]);
		
	return result;
		
    }

    private void addFeaturesToMap(TIntDoubleHashMap map, boolean negate) {
	if (null != subfv1) {
	    subfv1.addFeaturesToMap(map, false);

	    if (null != subfv2)
		subfv2.addFeaturesToMap(map, negateSecondSubFV);
	}

	ListIterator it = listIterator();
	if (negate) {
	    while (it.hasNext()) {
		Feature f = (Feature)it.next();
		if (!map.adjustValue(f.index, f.value))
		    map.put(f.index, -f.value);
	    }
	} else {
	    while (it.hasNext()) {
		Feature f = (Feature)it.next();
		if (!map.adjustValue(f.index, f.value))
		    map.put(f.index, f.value);
	    }
	}
    }


    public final String toString() {
	//StringBuilder sb = new StringBuilder();
	//ListIterator it = listIterator();
	//while (it.hasNext())
	//    sb.append(it.next().toString()).append(' ');
	//return sb.toString();
	return "TBA";
    }

}
