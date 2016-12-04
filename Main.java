import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Main
{
	private Object lock = new Object(); //Global lock
	
	public ArrayList<Long> turnaroundTimeArray = new ArrayList<Long>(); //To average out turnaroundTime
	public ArrayList<Long> responseTimeArray = new ArrayList<Long>(); //To average out repsonseTime
	
	public int numOfQueues = 0; //One thread per queue
	public int rangeOfQueueElements = 0; //Maximum range of queue length 
	
	public Main(int numOfQueues, int rangeOfQueueElements)
	{	
		this.numOfQueues = numOfQueues; //One thread per queue
		this.rangeOfQueueElements = rangeOfQueueElements; //Maximum range of queue length
	}
	
	/*
	 * Assumes all thread arrive at the same time and have predetermined queue size.
	 * Since size of the queues are already known, only the thread with the shortest queue will run, - 
	 * - and the rest of the thread will be put to wait().
	 */
	public ArrayList<Queue<Integer>> SJFqueues = new ArrayList<Queue<Integer>>(); //ArrayList of integer queues
	public int smallestValue = 0; // This will hold the queue with the smallest size
	class SJF extends Thread
	{
		public long firstRun = 0; //Time of first run
		public long startTime = 0; //Time of arrival
		public long endTime = 0; //Time completion
		
		public boolean globalBoolean = true; // Controls whether the thread continues or stops
		
		public Queue<Integer> queue = new LinkedList<Integer>(); // Used by the constructor to hold a particular queue for a thread
		
		public SJF(Queue<Integer> queue, String name)
		{
			super(name);
			this.queue = queue; // Sets the queue in this class to that of the one in the parameter
		}
		
		public void run()
		{
			startTime = System.currentTimeMillis();
			while(globalBoolean)
			{
				System.out.println(this.getName() + " is running and has size of " + queue.size()); //Used for testing
				synchronized(lock)
				{
					while(globalBoolean)
					{
						int smallestValue = SJFqueues.get(0).size(); //This sets smallestValue as the smallest queue size
						for (int i = 1; i < SJFqueues.size(); i++)
						{
							if (smallestValue > SJFqueues.get(i).size())
							{
								smallestValue = SJFqueues.get(i).size();
							}
						}
				
						/*
						 * If the queue used by the current thread is equal to smallestValue,
						 * then set this thread as the highest priority.
						 */
						if(queue.size() == smallestValue) 
						{
							this.setPriority(MAX_PRIORITY);
						}

						/*
						 * If the thread does not have the highest priority,
						 * then wait until it does.
						 * The thread will also keep updating smallestValue, and check if it 
						 * has the smallest queue size. 
						 */
						while(this.getPriority() != MAX_PRIORITY)
						{
							try
							{
								lock.wait();
								smallestValue = SJFqueues.get(0).size();
								for (int i = 1; i < SJFqueues.size(); i++)
								{
									if (smallestValue > SJFqueues.get(i).size())
									{
										smallestValue = SJFqueues.get(i).size();
									}
								}
								if(queue.size() == smallestValue)
								{
									this.setPriority(MAX_PRIORITY);
								}
							} 
							catch (InterruptedException e)
							{
								e.printStackTrace();
							}
						}
						
						/*
						 * If all the conditions above are satisfied, the thread will print out the values from its queue
						 * and it is polled() so the queue eventually empties.
						 */
						for(int i = 0; i < queue.size(); i++)
						{
							firstRun = System.currentTimeMillis();
							System.out.println(queue.poll());
						}
						
						/*
						 * Once the queue is empty, the thread will remove its queue from the queueArray, thus it is
						 * no longer capable of being the smallest queue. Therefore another queue will take its place.
						 */
						if(queue.isEmpty())
						{
							try
							{
								Thread.sleep(1000);
							} 
							catch (InterruptedException e)
							{
								e.printStackTrace();
							}
	
							for(int i = 0; i < SJFqueues.size(); i++)
							{
								if(SJFqueues.get(i).size() == queue.size())
								{
									SJFqueues.remove(i);
								}
							}
							globalBoolean = false; //This will end the thread
							lock.notifyAll(); //This will wake up waiting threads.
						}
					}
					
				}
			}
			endTime   = System.currentTimeMillis();
			
			long turnaroundTime = endTime - startTime;
			long responseTime = firstRun - startTime;
			
			turnaroundTimeArray.add(turnaroundTime);
			responseTimeArray.add(responseTime);
			
			System.out.println("turnaroundTime: " +  turnaroundTime);
			System.out.println("responseTime: " +  responseTime);
//			System.out.println(this.getName() + " is Closing");
			
			if(SJFqueues.isEmpty())
			{
				System.out.println("Average Turnaround Time: " + averageTurnaroundTime());
				System.out.println("Average Response Time: " + averageResponseTime());
			}
		}
	}
	
	/*
	 * My general plan for Round Robin is to have all the queues in an Array [queue1, queue2, queue3]
	 * A thread can only run if its queue is in the 0 index of the array.
	 * So, queue1 should run for some time slice, lets say 1ms.
	 * After 1ms, it should switch to another thread by removing queue1 from the array, and then adding it to the back.
	 * Since queue1 is no longer in index 0, queue2 should run. After queue 2 runs for 1ms, it should be put to the back of the array.
	 * And then queue3 should run. This is repeated until all queues are emptied.
	 */
	public ArrayList<Queue<Integer>> RRqueues = new ArrayList<Queue<Integer>>();
	class RR extends Thread
	{
		public int firstRunCount = 0; //Needed due to cycling.
		
		public long firstRun = 0; //Time of first run
		public long startTime = 0; //Time of arrival
		public long endTime = 0; //Time of completion
		
		public long turnaroundTime;
		public long responseTime;
		
		public boolean globalBoolean = true; //Determines whether a thread stops or continues running
		
		public Queue<Integer> queue = new LinkedList<Integer>(); //instance of the queue that will be passed in through the constructor
		
		public RR(Queue<Integer> queue, String name)
		{
			super(name);
			this.queue = queue;
		}
		
		public void run() 
		{
			startTime = System.currentTimeMillis();
			while(globalBoolean)
			{
				System.out.println(this.getName() + " is running and has size of " + queue.size()); //Used for testing
				synchronized(lock)
				{
					while(globalBoolean && !RRqueues.isEmpty()) //Loop only runs IF RRqueues is NOT empty. If this isn't clarified, the program will crash.
					{
						while(!queue.equals(RRqueues.get(0))) //Checks if this queue is NOT equal to the queue in the RRqueues Array at index 0.
						{
							try
							{
								lock.wait(); //If the condition above is true, this thread will spin wait.
							} 
							catch (InterruptedException e)
							{
								e.printStackTrace();
							}
						}
						
						if(queue.equals(RRqueues.get(0))) //If this queue is equal to the queue in the RRqueues Array at index 0, then proceed.
						{
							if(firstRunCount == 0) //We want firstRun, not second or third.
							{
								firstRun = System.currentTimeMillis();
								responseTime = firstRun - startTime;
								firstRunCount++;
							}
							
							long start = System.currentTimeMillis(); //start time
							long timeSlice = 1; //time slice time
							long end = start + timeSlice; 
							int timeSliceCount = 0; //For test purposes
							
							while (System.currentTimeMillis() < end && queue.peek() != null) { //This loop will run for a certain time slice.
								System.out.println(queue.poll()); //Polls from this queue, for a certain amount of time.
								timeSliceCount++; //For test purposes, shows the actual number of items that has been polled.
							}
							
							System.out.println("Count: " + timeSliceCount);
							
							try
							{
								Thread.sleep(1000); //Make the thread sleep for a bit, so I can track the output.
							} 
							catch (InterruptedException e)
							{
								e.printStackTrace();
							}
							
							if(queue.isEmpty()) //If the queue is empty
							{
								RRqueues.remove(0); //Remove this queue, which should be at index 0.
								globalBoolean = false; //And also discontinue the while loop.
							}
							
							else if(!queue.isEmpty()) //If the queue is NOT empty
							{
								Queue<Integer> dummyQueue = new LinkedList<Integer>(); //Create dummy queue
								dummyQueue = queue; //Set dummy queue as this queue
								RRqueues.remove(queue); //Remove this queue from RRqueues Array, this will shift the elements so that item at index 1 becomes index 0
								RRqueues.add(dummyQueue); //Adds this queue back into RRqueues Array so that it is the LAST item in the array.
							}
							lock.notifyAll(); //Wakes the threads.
						}
					}
				}
			}
			endTime   = System.currentTimeMillis();
			
			turnaroundTime = endTime - startTime;
			
			turnaroundTimeArray.add(turnaroundTime);
			responseTimeArray.add(responseTime);
			
			System.out.println("turnaroundTime: " +  turnaroundTime);
			System.out.println("responseTime: " +  responseTime);
			
			if(RRqueues.isEmpty())
			{
				System.out.println("Average Turnaround Time: " + averageTurnaroundTime());
				System.out.println("Average Response Time: " + averageResponseTime());
			}
		}
	}
	
	ArrayList<SJF> SJFlist = new ArrayList<SJF>(); //Holds SJF threads
	public void simulationSJF()
	{
		Random rand = new Random();
		
		for(int i = 0; i < numOfQueues; i++)
		{
			int randomNum = rand.nextInt(rangeOfQueueElements) + 50; //Creates random integer with base of 50 and upper limit of "rangeOfQueueElements"
			Queue<Integer> queue = new LinkedList<Integer>(); //Creates queue
			for(int j = 0; j < randomNum; j++)
			{
				queue.add(randomNum); //adds the generated number into the queue 
			}
			SJFqueues.add(queue); //adds the queue into the ArrayList of queues
		}
		
		for(int i = 0; i < numOfQueues; i++) //For the amount of Queues generated (1 thread per queue)
		{
			String strName = "Thread";
			int intName = i;
			String name = strName + intName;
			SJFlist.add(new SJF(SJFqueues.get(i), name)); //Creates SJF objects with it's respective queue, and add them to an ArrayList of SJF objects
		}
		
		for(int i = 0; i < numOfQueues; i++) //Starts all the threads.
		{
			SJFlist.get(i).start(); 
		}
	}
	
	ArrayList<RR> RRlist = new ArrayList<RR>(); //Holds RR threads
	public void simulationRR()
	{
		Random rand = new Random();
		
		for(int i = 0; i < numOfQueues; i++)
		{
			int randomNum = rand.nextInt(rangeOfQueueElements) + 50;
			Queue<Integer> queue = new LinkedList<Integer>();
			for(int j = 0; j < randomNum; j++)
			{
				queue.add(randomNum);
			}
			RRqueues.add(queue);
		}
		
		for(int i = 0; i < numOfQueues; i++)
		{
			String strName = "Thread";
			int intName = i;
			String name = strName + intName;
			RRlist.add(new RR(RRqueues.get(i), name));
		}
		
		for(int i = 0; i < numOfQueues; i++)
		{
			RRlist.get(i).start();
		}
	}

	private Long averageTurnaroundTime()
	{
		long total = 0;
		
		for(int i = 0; i < turnaroundTimeArray.size(); i++)
		{
			total += turnaroundTimeArray.get(i);
		}
		
		total = total / turnaroundTimeArray.size();
		
		return total;
	}
	
	private Long averageResponseTime()
	{
		long total = 0;
		
		for(int i = 0; i < responseTimeArray.size(); i++)
		{
			total += responseTimeArray.get(i);
		}
		
		total = total / responseTimeArray.size();
		
		return total;
	}
	
	public static void main(String[] args)
	{	
		/*
		 * Do not run simulationSJF and simulationRR at the same time.
		 * First parameter is for the number of threads, and second parameter sets the highest number for some random integer.
		 * Base number of elements in queues set at 50. 
		 */
		
//		Main testSJF = new Main(3, 10); 
//		testSJF.simulationSJF();

//		Main testRR = new Main(3, 10);
//		testRR.simulationRR();	
		
//		Main testSJF = new Main(5, 500); 
//		testSJF.simulationSJF();

//		Main testRR = new Main(5, 500);
//		testRR.simulationRR();	
		
		
	}

}
