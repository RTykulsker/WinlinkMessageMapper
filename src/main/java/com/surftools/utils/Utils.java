/**

The MIT License (MIT)

Copyright (c) 2023, Robert Tykulsker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/

package com.surftools.utils;

import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class Utils {

  /**
   * prompt for user input
   *
   * @param prompt
   * @param timeoutSeconds
   * @param predicate
   *
   * @return false if timed out, otherwise result of evaluating predicate on user input
   */
  @SuppressWarnings("resource")
  public static boolean promptForInputWithTimeout(String prompt, int timeoutSeconds, Predicate<String> predicate) {
    var response = "";
    Callable<String> callable = () -> new Scanner(System.in).next();

    ExecutorService service = Executors.newFixedThreadPool(1);

    System.out.print(prompt);

    Future<String> inputFuture = service.submit(callable);

    try {
      response = inputFuture.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Thread was interrupted", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Something went wrong", e);
    } catch (TimeoutException e) {
      System.err.println("timeout!");
      return false;
    } finally {
      service.shutdown();
    }

    if (response == null) {
      return false;
    }

    return predicate.test(response);
  }
}
