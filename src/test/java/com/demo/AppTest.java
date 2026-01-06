package com.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {
  @Test
  void hello_shouldWork() {
    assertEquals("hola Jose", App.hello("Jose"));
  }
}
