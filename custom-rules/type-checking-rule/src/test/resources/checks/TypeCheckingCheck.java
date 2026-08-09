class Bird { // Noncompliant {{Type-code field 'type' drives a switch statement in 2 different methods of this class. Consider Replace Conditional with Polymorphism or Replace Type Code with State/Strategy.}}

  enum Type { EUROPEAN, AFRICAN, NORWEGIAN_BLUE }

  private Type type;
  private boolean isNailed;
  private int numberOfCoconuts;

  double getSpeed() {
    switch (type) {
      case EUROPEAN:
        return getBaseSpeed();
      case AFRICAN:
        return getBaseSpeed() - getLoadFactor() * numberOfCoconuts;
      case NORWEGIAN_BLUE:
        return isNailed ? 0 : getBaseSpeed();
      default:
        throw new IllegalStateException();
    }
  }

  String getPlumageDescription() {
    switch (type) {
      case EUROPEAN:
        return "blue and white";
      case AFRICAN:
        return "unladen";
      case NORWEGIAN_BLUE:
        return "beautiful plumage";
      default:
        throw new IllegalStateException();
    }
  }

  private double getBaseSpeed() { return 10.0; }
  private double getLoadFactor() { return 1.5; }
}

class SingleSwitchIsFine { // Compliant, only one method switches on the field

  enum Kind { A, B }
  private Kind kind;

  String describe() {
    switch (kind) {
      case A: return "a";
      case B: return "b";
      default: return "?";
    }
  }
}

class AlreadyPolymorphic { // Compliant, no type-code field at all

  interface Shape {
    double area();
  }

  static class Circle implements Shape {
    double radius;
    public double area() { return Math.PI * radius * radius; }
  }

  static class Square implements Shape {
    double side;
    public double area() { return side * side; }
  }
}

class InstanceofChainExample {

  void process(Object shape) { // Noncompliant {{This if/else-if chain tests the type of 'shape' in 3 branches. Consider Replace Conditional with Polymorphism.}}
    if (shape instanceof Circle) {
      handleCircle((Circle) shape);
    } else if (shape instanceof Square) {
      handleSquare((Square) shape);
    } else if (shape instanceof Triangle) {
      handleTriangle((Triangle) shape);
    }
  }

  void processShort(Object shape) { // Compliant, only 2 branches, below default threshold of 3

    if (shape instanceof Circle) {
      handleCircle((Circle) shape);
    } else if (shape instanceof Square) {
      handleSquare((Square) shape);
    }
  }

  private void handleCircle(Circle c) { }
  private void handleSquare(Square s) { }
  private void handleTriangle(Triangle t) { }

  static class Circle { }
  static class Square { }
  static class Triangle { }
}
