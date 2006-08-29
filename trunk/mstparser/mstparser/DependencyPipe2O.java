package mstparser;

import java.io.*;
import gnu.trove.*;
import java.util.*;
import mstparser.io.*;

public class DependencyPipe2O extends DependencyPipe {

    public DependencyPipe2O() throws IOException {
	super();
    }

    public DependencyPipe2O(boolean createForest, String format) throws IOException {
	super(createForest, format);
    }

    public FeatureVector createFeatureVector(DependencyInstance instance,
					     int par,
					     int ch1, int ch2,
					     FeatureVector fv) {

	String[] forms = instance.forms;
	String[] pos = instance.postags;
		
	// ch1 is always the closes to par
	String dir = par > ch2 ? "RA" : "LA";
		
	String par_pos = pos[par];
	String ch1_pos = ch1 == par ? "STPOS" : pos[ch1];
	String ch2_pos = pos[ch2];
	String ch1_word = ch1 == par ? "STWRD" : forms[ch1];
	String ch2_word = forms[ch2];

	String pTrip = par_pos+"_"+ch1_pos+"_"+ch2_pos;
	fv = add("POS_TRIP="+pTrip+"_"+dir,1.0,fv);
	fv = add("APOS_TRIP="+pTrip,1.0,fv);
		
	return fv;
    }
	
    public FeatureVector createFeatureVectorSib(DependencyInstance instance,
						int ch1, int ch2,
						boolean isST,
						FeatureVector fv) {

	String[] forms = instance.forms;
	String[] pos = instance.postags;
		
	// ch1 is always the closes to par
	String dir = ch1 > ch2 ? "RA" : "LA";
		
	String ch1_pos = isST ? "STPOS" : pos[ch1];
	String ch2_pos = pos[ch2];
	String ch1_word = isST ? "STWRD" : forms[ch1];
	String ch2_word = forms[ch2];

	fv = add("CH_PAIR="+ch1_pos+"_"+ch2_pos+"_"+dir,1.0,fv);
	fv = add("CH_WPAIR="+ch1_word+"_"+ch2_word+"_"+dir,1.0,fv);
	fv = add("CH_WPAIRA="+ch1_word+"_"+ch2_pos+"_"+dir,1.0,fv);
	fv = add("CH_WPAIRB="+ch1_pos+"_"+ch2_word+"_"+dir,1.0,fv);
	fv = add("ACH_PAIR="+ch1_pos+"_"+ch2_pos,1.0,fv);
	fv = add("ACH_WPAIR="+ch1_word+"_"+ch2_word,1.0,fv);
	fv = add("ACH_WPAIRA="+ch1_word+"_"+ch2_pos,1.0,fv);
	fv = add("ACH_WPAIRB="+ch1_pos+"_"+ch2_word,1.0,fv);

	int dist = Math.max(ch1,ch2)-Math.min(ch1,ch2);
	String distBool = "0";
	if(dist > 1)
	    distBool = "1";
	if(dist > 2)
	    distBool = "2";
	if(dist > 3)
	    distBool = "3";
	if(dist > 4)
	    distBool = "4";
	if(dist > 5)
	    distBool = "5";
	if(dist > 10)
	    distBool = "10";		
	fv = add("SIB_PAIR_DIST="+distBool+"_"+dir,1.0,fv);
	fv = add("ASIB_PAIR_DIST="+distBool,1.0,fv);
	fv = add("CH_PAIR_DIST="+ch1_pos+"_"+ch2_pos+"_"+distBool+"_"+dir,1.0,fv);
	fv = add("ACH_PAIR_DIST="+ch1_pos+"_"+ch2_pos+"_"+distBool,1.0,fv);
				
		
	return fv;
    }
			
    public FeatureVector createFeatureVector(DependencyInstance instance, int[] labs1) {

	String[] labs = new String[labs1.length];
	for(int i = 0; i < labs.length; i++)
	    labs[i] = types[labs1[i]];

	instance.deprels = labs;

	return createFeatureVector(instance);	    
    }

    public FeatureVector createFeatureVector(DependencyInstance instance) {
	    
	String[] forms = instance.forms;
	String[] labs = instance.deprels;
	int[] heads = instance.heads;	

	FeatureVector fv = new FeatureVector();
	for(int i = 0; i < forms.length; i++) {
	    if(heads[i] == -1)
		continue;
	    int small = i < heads[i] ? i : heads[i];
	    int large = i > heads[i] ? i : heads[i];
	    boolean attR = i < heads[i] ? false : true;
	    fv = createFeatureVector(instance,small,large,attR,fv);
	    if(labeled) {
		fv = createFeatureVector(instance,i,labs[i],attR,true,fv);
		fv = createFeatureVector(instance,heads[i],labs[i],attR,false,fv);
	    }
	}
	// find all trip features
	for(int i = 0; i < forms.length; i++) {
	    if(heads[i] == -1 && i != 0) continue;
	    // right children
	    int prev = i;
	    for(int j = i+1; j < forms.length; j++) {
		if(heads[j] == i) {
		    fv = createFeatureVector(instance,i,prev,j,fv);
		    fv = createFeatureVectorSib(instance,prev,j,prev==i,fv);
		    prev = j;
		}
	    }
	    prev = i;
	    for(int j = i-1; j >= 0; j--) {
		if(heads[j] == i) {
		    fv = createFeatureVector(instance,i,prev,j,fv);
		    fv = createFeatureVectorSib(instance,prev,j,prev==i,fv);
		    prev = j;
		}
	    }
	}
		
	return fv;
    }

    public void possibleFeatures(DependencyInstance instance, ObjectOutputStream out) {

	String[] forms = instance.forms;
		
	try {

	    for(int w1 = 0; w1 < forms.length; w1++) {
		for(int w2 = w1+1; w2 < forms.length; w2++) {
					
		    for(int ph = 0; ph < 2; ph++) {						
			boolean attR = ph == 0 ? true : false;
							
			FeatureVector prodFV = createFeatureVector(instance,w1,w2,attR,
								   new FeatureVector());
						
			for(FeatureVector curr = prodFV; curr != null; curr = curr.next) {
			    if(curr.index >= 0)
				out.writeInt(curr.index);
			}
			out.writeInt(-2);
		    }
		}
			
	    }

	    out.writeInt(-3);

	    if(labeled) {
		for(int w1 = 0; w1 < forms.length; w1++) {
		    
		    for(int t = 0; t < types.length; t++) {
			String type = types[t];
			
			for(int ph = 0; ph < 2; ph++) {						
			    boolean attR = ph == 0 ? true : false;
			    
			    for(int ch = 0; ch < 2; ch++) {						
				boolean child = ch == 0 ? true : false;						
				
				FeatureVector prodFV = createFeatureVector(instance,w1,
									   type,
									   attR,child,
									   new FeatureVector());
				
				for(FeatureVector curr = prodFV; curr != null; curr = curr.next) {
				    if(curr.index >= 0)
					out.writeInt(curr.index);
				}
				out.writeInt(-2);
				
			    }
			}
		    }
		    
		}
		
		out.writeInt(-3);
	    }

	    for(int w1 = 0; w1 < forms.length; w1++) {
		for(int w2 = w1; w2 < forms.length; w2++) {
		    for(int w3 = w2+1; w3 < forms.length; w3++) {
			FeatureVector prodFV = createFeatureVector(instance,w1,w2,w3,
								   new FeatureVector());
			for(FeatureVector curr = prodFV; curr != null; curr = curr.next) {
			    if(curr.index >= 0)
				out.writeInt(curr.index);
			}
			out.writeInt(-2);
		    }
		}
		for(int w2 = w1; w2 >= 0; w2--) {
		    for(int w3 = w2-1; w3 >= 0; w3--) {
			FeatureVector prodFV = createFeatureVector(instance,w1,w2,w3,
								   new FeatureVector());
			for(FeatureVector curr = prodFV; curr != null; curr = curr.next) {
			    if(curr.index >= 0)
				out.writeInt(curr.index);
			}
			out.writeInt(-2);
		    }
		}
	    }
			
	    out.writeInt(-3);
			
	    for(int w1 = 0; w1 < forms.length; w1++) {
		for(int w2 = 0; w2 < forms.length; w2++) {
		    for(int wh = 0; wh < 2; wh++) {
			if(w1 != w2) {
			    FeatureVector prodFV = createFeatureVectorSib(instance,w1,w2,wh == 0,
									  new FeatureVector());
			    for(FeatureVector curr = prodFV; curr != null; curr = curr.next) {
				if(curr.index >= 0)
				    out.writeInt(curr.index);
			    }
			    out.writeInt(-2);
			}
		    }
		}
	    }

	    out.writeInt(-3);
						
	    for(FeatureVector curr = instance.fv; curr.next != null; curr = curr.next)
		out.writeInt(curr.index);

	    out.writeObject(instance);

	    out.writeInt(-1);
	    out.reset();

	} catch (IOException e) {}
		
    }

    public DependencyInstance getFeatureVector(ObjectInputStream in,
					       int length,
					       FeatureVector[][][] fvs,
					       double[][][] probs,
					       FeatureVector[][][] fvs_trips,
					       double[][][] probs_trips,
					       FeatureVector[][][] fvs_sibs,
					       double[][][] probs_sibs,
					       FeatureVector[][][][] nt_fvs,
					       double[][][][] nt_probs,
					       Parameters params) throws IOException {

	// Get production crap.		
	for(int w1 = 0; w1 < length; w1++) {
	    for(int w2 = w1+1; w2 < length; w2++) {
				
		for(int ph = 0; ph < 2; ph++) {

		    FeatureVector prodFV = new FeatureVector();
					
		    int indx = in.readInt();
		    while(indx != -2) {
			prodFV = new FeatureVector(indx,1.0,prodFV);
			indx = in.readInt();
		    }
					
		    double prodProb = params.getScore(prodFV);
		    fvs[w1][w2][ph] = prodFV;
		    probs[w1][w2][ph] = prodProb;
		}
	    }
			
	}
	int last = in.readInt();
	if(last != -3) { System.out.println("Error reading file."); System.exit(0); }

	if(labeled) {
	    for(int w1 = 0; w1 < length; w1++) {
		
		for(int t = 0; t < types.length; t++) {
		    String type = types[t];
		    
		    for(int ph = 0; ph < 2; ph++) {						
			
			for(int ch = 0; ch < 2; ch++) {						
			    
			    FeatureVector prodFV = new FeatureVector();
			    
			    int indx = in.readInt();
			    while(indx != -2) {
				prodFV = new FeatureVector(indx,1.0,prodFV);
				indx = in.readInt();
			    }
			    
			    double nt_prob = params.getScore(prodFV);
			    nt_fvs[w1][t][ph][ch] = prodFV;
			    nt_probs[w1][t][ph][ch] = nt_prob;
			    
			}
		    }
		}
		
	    }
	    last = in.readInt();
	    if(last != -3) { System.out.println("Error reading file."); System.exit(0); }
	}

	for(int w1 = 0; w1 < length; w1++) {
	    for(int w2 = w1; w2 < length; w2++) {
		for(int w3 = w2+1; w3 < length; w3++) {
		    FeatureVector prodFV = new FeatureVector();
		    
		    int indx = in.readInt();
		    while(indx != -2) {
			prodFV = new FeatureVector(indx,1.0,prodFV);
			indx = in.readInt();
		    }

		    double prodProb = params.getScore(prodFV);
		    fvs_trips[w1][w2][w3] = prodFV;
		    probs_trips[w1][w2][w3] = prodProb;
		    

		}
	    }
	    for(int w2 = w1; w2 >= 0; w2--) {
		for(int w3 = w2-1; w3 >= 0; w3--) {
		    FeatureVector prodFV = new FeatureVector();
		    
		    int indx = in.readInt();
		    while(indx != -2) {
			prodFV = new FeatureVector(indx,1.0,prodFV);
			indx = in.readInt();
		    }

		    double prodProb = params.getScore(prodFV);
		    fvs_trips[w1][w2][w3] = prodFV;
		    probs_trips[w1][w2][w3] = prodProb;

		}
	    }
	}
			
	last = in.readInt();
	if(last != -3) { System.out.println("Error reading file."); System.exit(0); }
			
	for(int w1 = 0; w1 < length; w1++) {
	    for(int w2 = 0; w2 < length; w2++) {
		for(int wh = 0; wh < 2; wh++) {
		    if(w1 != w2) {
			FeatureVector prodFV = new FeatureVector();
			
			int indx = in.readInt();
			while(indx != -2) {
			    prodFV = new FeatureVector(indx,1.0,prodFV);
			    indx = in.readInt();
			}

			double prodProb = params.getScore(prodFV);
			fvs_sibs[w1][w2][wh] = prodFV;
			probs_sibs[w1][w2][wh] = prodProb;

		    }
		}
	    }
	}

	last = in.readInt();
	if(last != -3) { System.out.println("Error reading file."); System.exit(0); }

	FeatureVector nfv = new FeatureVector();
	int next = in.readInt();
	while(next != -4) {
	    nfv = new FeatureVector(next,1.0,nfv);
	    next = in.readInt();
	}

	DependencyInstance marshalledDI;
	try {
	    marshalledDI = (DependencyInstance)in.readObject();
	    marshalledDI.setFeatureVector(nfv);	
	    next = in.readInt();
	    if(next != -1) { 
		System.out.println("Error reading file."); System.exit(0); 
	    }
	    return marshalledDI;
	} catch(ClassNotFoundException e) { 
	    System.out.println("Error reading file."); System.exit(0); 
	} finally {
	    // this won't happen, but it takes care of compilation complaints
	    return null;
	}
		
    }
		
    public void getFeatureVector(DependencyInstance instance,
				 FeatureVector[][][] fvs,
				 double[][][] probs,
				 FeatureVector[][][] fvs_trips,
				 double[][][] probs_trips,
				 FeatureVector[][][] fvs_sibs,
				 double[][][] probs_sibs,
				 FeatureVector[][][][] nt_fvs,
				 double[][][][] nt_probs, Parameters params) {

	String[] forms = instance.forms;
	String[] pos = instance.postags;
	String[] labs = instance.deprels;
		
	// Get production crap.		
	for(int w1 = 0; w1 < forms.length; w1++) {
	    for(int w2 = w1+1; w2 < forms.length; w2++) {
				
		for(int ph = 0; ph < 2; ph++) {
		    boolean attR = ph == 0 ? true : false;
		    
		    int childInt = attR ? w2 : w1;
		    int parInt = attR ? w1 : w2;
		    
		    FeatureVector prodFV = createFeatureVector(instance,w1,w2,attR,
							       new FeatureVector());
										
		    double prodProb = params.getScore(prodFV);
		    fvs[w1][w2][ph] = prodFV;
		    probs[w1][w2][ph] = prodProb;
		}
	    }
			
	}

	if(labeled) {
	    for(int w1 = 0; w1 < forms.length; w1++) {
		
		for(int t = 0; t < types.length; t++) {
		    String type = types[t];
		    
		    for(int ph = 0; ph < 2; ph++) {						
			boolean attR = ph == 0 ? true : false;
			
			for(int ch = 0; ch < 2; ch++) {						
			    boolean child = ch == 0 ? true : false;						
			    
			    FeatureVector prodFV = createFeatureVector(instance,w1,
								       type,attR,child,
								       new FeatureVector());
			    
			    double nt_prob = params.getScore(prodFV);
			    nt_fvs[w1][t][ph][ch] = prodFV;
			    nt_probs[w1][t][ph][ch] = nt_prob;
			    
			}
		    }
		}
		
	    }
	}

		
	for(int w1 = 0; w1 < forms.length; w1++) {
	    for(int w2 = w1; w2 < forms.length; w2++) {
		for(int w3 = w2+1; w3 < forms.length; w3++) {
		    FeatureVector prodFV = createFeatureVector(instance,w1,w2,w3,
							       new FeatureVector());
		    double prodProb = params.getScore(prodFV);
		    fvs_trips[w1][w2][w3] = prodFV;
		    probs_trips[w1][w2][w3] = prodProb;
		}
	    }
	    for(int w2 = w1; w2 >= 0; w2--) {
		for(int w3 = w2-1; w3 >= 0; w3--) {
		    FeatureVector prodFV = createFeatureVector(instance,w1,w2,w3,
							       new FeatureVector());
		    double prodProb = params.getScore(prodFV);
		    fvs_trips[w1][w2][w3] = prodFV;
		    probs_trips[w1][w2][w3] = prodProb;
		}
	    }
	}
			
	for(int w1 = 0; w1 < forms.length; w1++) {
	    for(int w2 = 0; w2 < forms.length; w2++) {
		for(int wh = 0; wh < 2; wh++) {
		    if(w1 != w2) {
			FeatureVector prodFV = createFeatureVectorSib(instance,w1,w2,wh == 0,
								      new FeatureVector());
			double prodProb = params.getScore(prodFV);
			fvs_sibs[w1][w2][wh] = prodFV;
			probs_sibs[w1][w2][wh] = prodProb;
		    }
		}
	    }
	}
    }
		
}
