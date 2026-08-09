class OrderProcessor { // Noncompliant [[sc=7;ec=21]] {{Field 'discount' is null-checked in 3 different methods of this class. Consider introducing a Null Object (or java.util.Optional) so callers no longer need to repeat this check.}}

  private Discount discount;

  void applyDiscount(Order order) {
    if (discount != null) {
      order.setTotal(discount.apply(order.getTotal()));
    }
  }

  String describeDiscount() {
    if (discount != null) {
      return discount.getDescription();
    }
    return "No discount";
  }

  boolean hasDiscount() {
    return discount != null;
  }
}

class SingleCheckIsFine { // Compliant, only one method checks the field

  private Discount discount;

  void applyDiscount(Order order) {
    if (discount != null) {
      order.setTotal(discount.apply(order.getTotal()));
    }
  }
}

class LocalVariableIsIgnored { // Compliant, "discount" here is a local, not a field

  void method1() {
    Discount discount = compute();
    if (discount != null) {
      discount.apply(0);
    }
  }

  void method2() {
    Discount discount = compute();
    if (discount != null) {
      discount.apply(0);
    }
  }

  void method3() {
    Discount discount = compute();
    if (discount != null) {
      discount.apply(0);
    }
  }

  private Discount compute() {
    return null;
  }
}

class AlreadyRefactored { // Compliant, no null checks at all — Null Object already in use

  private Discount discount = Discount.NONE;

  void applyDiscount(Order order) {
    order.setTotal(discount.apply(order.getTotal()));
  }

  String describeDiscount() {
    return discount.getDescription();
  }

  boolean hasDiscount() {
    return discount.isPresent();
  }
}

interface Discount {
  Discount NONE = new Discount() {
    public double apply(double total) { return total; }
    public String getDescription() { return "No discount"; }
    public boolean isPresent() { return false; }
  };
  double apply(double total);
  String getDescription();
  boolean isPresent();
}

interface Order {
  double getTotal();
  void setTotal(double total);
}
