//  Copyright (c) 2021 Christopher Taylor
//
//  Distributed under the Boost Software License, Version 1.0. (See accompanying
//  file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
import org.jmq.collectives.Params;
import org.jmq.collectives.BasicTcpBackend;

import java.lang.ClassNotFoundException;
import java.io.IOException;
import java.util.Vector;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    public static void main(String [] args)
    {
  
        Params p = new Params();
        System.out.println(p.rank());
        System.out.println(p.n_ranks());

        BasicTcpBackend be = new BasicTcpBackend(p);
        be.initialize(p);

        {
            // broadcast pushes data from 0
            // to all other ranks
            //
            System.out.println("broadcast");
            int val = 0;

            if(be.rank() == 0) {

                // have rank 0 sleep
                // to test heartbeat
                //
                //try {
                //    Thread.sleep(10000);
                //} catch(Exception e) { e.printStackTrace(); }

                val = 1;
            }

            try {
                val = be.broadcast(val);
                System.out.println(val);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            catch(ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

        System.out.println("barrier");
        try {
            be.barrier();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(ClassNotFoundException e) {
            e.printStackTrace();
        }

        {
            // reduction computes the result
            // to rank 0
            //
            System.out.println("reduce");
            Vector<Integer> values = new Vector<Integer>();
            values.addElement(1);
            values.addElement(1);
            values.addElement(1);
            values.addElement(1);

            if( be.rank() == 0 ) {
                // have rank 0 sleep longer
                // to test heartbeat
                //
                try {
                    Thread.sleep(32000);
                } catch(Exception e) { e.printStackTrace(); }
            }

            try {
                Integer val = be.reduce(0, (x1, x2) -> x1 + x2, values.stream());
                System.out.println(val);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            catch(ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

        try {
            be.barrier();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(ClassNotFoundException e) {
            e.printStackTrace();
        }

        {
            // scatter pushes from rank 0 to
            // other ranks across the network
            //
            System.out.println("scatter");
            Vector<Integer> ivalues = new Vector<Integer>();
            ivalues.addElement(1);
            ivalues.addElement(1);
            ivalues.addElement(1);
            ivalues.addElement(1);

            java.util.stream.Stream<Integer> ovalues_stream = null;

            try {
                ovalues_stream = be.scatter(ivalues.iterator(), ivalues.size());
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            catch(ClassNotFoundException e) {
                e.printStackTrace();
            }

            Vector<Integer> ovalues_res = ovalues_stream.collect(java.util.stream.Collectors.toCollection(Vector<Integer>::new));
       
            System.out.println("ovalues\t" + ovalues_res.size());
            for(Integer iv : ovalues_res) {
                System.out.println(iv);
            }
        }

        try {
            be.barrier();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(ClassNotFoundException e) {
            e.printStackTrace();
        }

        {
            System.out.println("gather");
            Vector<Integer> ivalues = new Vector<Integer>();
            ivalues.addElement((int)be.rank());
            ivalues.addElement((int)be.rank());
            ivalues.addElement((int)be.rank());
            ivalues.addElement((int)be.rank());

            java.util.stream.Stream<Integer> ovalues_stream = null;

            try {
                ovalues_stream = be.gather(ivalues.iterator(), ivalues.size());
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            catch(ClassNotFoundException e) {
                e.printStackTrace();
            }

            // gather consolidates to rank 0
            //
            //if(ovalues_stream != null && be.rank() < 1 ) {
                Vector<Integer> ovalues_res = ovalues_stream.collect(java.util.stream.Collectors.toCollection(Vector<Integer>::new));
       
                System.out.println("ovalues\t" + ovalues_res.size());
                for(Integer iv : ovalues_res) {
                    System.out.println(iv);
                }
            //}
        }

        System.out.println("barrier");
        try {
            be.barrier();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(ClassNotFoundException e) {
            e.printStackTrace();
        }

        be.finalize();
    }
}
