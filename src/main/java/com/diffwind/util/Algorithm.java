package com.diffwind.util;

public class Algorithm {

	public static int[] bubbleSortDescending(double[] arr){
		//排序后位置－>元素索引
		int[] order = new int[arr.length];
		//元素索引－>位置
		
		for (int i = 0; i < arr.length;i++)
			order[i] = i;
		
		int temp;
		for(int i=0; i < arr.length-1; i++){
			
			for(int j=1; j < arr.length-i; j++){//最小值交换到最后
				/*if(arr[j-1] < arr[j]){
					temp=arr[j-1];
					arr[j-1] = arr[j];
					arr[j] = temp;*/
					
				if(arr[order[j-1]] < arr[order[j]]){
				
					//int t;
					temp = order[j-1];
					order[j-1] = order[j];
					order[j] = temp;
				}
			}
			//check that last index has highest value in first loop,
			// second last index has second last highest value and so on
			//System.out.println("Array after "+(i+1)+"th iteration:"+Arrays.toString(arr));
			//System.out.println("Order after "+(i+1)+"th iteration:"+Arrays.toString(order));
		}
		
		return order;
	}
	
	public static int[] bubbleSortAscending(double[] arr){
		//排序后位置－>元素索引
		int[] order = new int[arr.length];
		//元素索引－>位置
		
		for (int i = 0; i < arr.length;i++)
			order[i] = i;
		
		int temp;
		for(int i=0; i < arr.length-1; i++){
			
			for(int j=1; j < arr.length-i; j++){//最大值交换到最后
				/*if(arr[j-1] < arr[j]){
					temp=arr[j-1];
					arr[j-1] = arr[j];
					arr[j] = temp;*/
					
				if(arr[order[j-1]] > arr[order[j]]){
				
					//int t;
					temp = order[j-1];
					order[j-1] = order[j];
					order[j] = temp;
				}
			}
			//check that last index has highest value in first loop,
			// second last index has second last highest value and so on
			//System.out.println("Array after "+(i+1)+"th iteration:"+Arrays.toString(arr));
			//System.out.println("Order after "+(i+1)+"th iteration:"+Arrays.toString(order));
		}
		
		return order;
	}
}
