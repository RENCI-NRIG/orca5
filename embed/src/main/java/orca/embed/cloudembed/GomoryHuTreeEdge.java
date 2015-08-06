package orca.embed.cloudembed;

import orca.ndl.LayerConstant;

public class GomoryHuTreeEdge implements Comparable{
	
		String name;
		int sn;
		float weight;
		int [] S1;
		int [] S2;
		int [] cutEdges;
		
	public GomoryHuTreeEdge(String n,float w,int []s1, int [] s2, int [] edges){
		name = n;
		weight = w;
		S1=s1;
		S2=s2;
		cutEdges = edges;
	}
	
	public GomoryHuTreeEdge(int n,float w,int []s1, int [] s2, int [] edge){
		sn=n;
		weight = w;
		S1=s1;
		S2=s2;
		cutEdges = edge;
	}
	
	public int getSN(){
		return sn;
	}
	
	public void setName(String n){
		name = n;
	}
	
	public void print(){
		System.out.println("SN="+sn+";Weight="+weight);
		if(S1!=null){
			System.out.println("S1=");
			for(int i=0;i<S1.length;i++){
				System.out.println(S1[i]+";");
			}
		}
		if(S2!=null){
			System.out.println("\nS2=");
			for(int i=0;i<S2.length;i++){
				System.out.println(S2[i]+";");
			}
		}
		if(cutEdges!=null){
			System.out.println("\ncutEdges=");
			for(int i=0;i<cutEdges.length;i++){
				System.out.println(cutEdges[i]+";");
			}
		}
	}

	public int compareTo(Object o) {
		int compare=0;

		if(o==null) return 1;
		
		 GomoryHuTreeEdge newE = (GomoryHuTreeEdge) o;
		 
		 if(this.weight > newE.weight)
			 compare = 1;
		 if(this.weight < newE.weight)
			 compare = -1;
		 
		 if(this.weight == newE.weight){
			 if(this.cutEdges.length > newE.cutEdges.length)
				 compare = 1;
			 if(this.cutEdges.length < newE.cutEdges.length)
				 compare = -1;
		 }
		
		return compare;
	}
}
