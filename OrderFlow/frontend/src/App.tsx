import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Area, AreaChart, Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

type Product = {
  id: string;
  name: string;
  category: string;
  price: number;
  sku: string;
  active: boolean;
  organizationId: string;
};

type Order = {
  id: string;
  status: string;
  totalAmount: number;
  currency: string;
};

export default function App() {
  const [mode, setMode] = useState<'real' | 'demo'>('real');
  const [products, setProducts] = useState<Product[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusMessage, setStatusMessage] = useState('');
  const [token, setToken] = useState(() => localStorage.getItem('orderflow_token') ?? '');
  const [email, setEmail] = useState('admin@acme.test');
  const [password, setPassword] = useState('demo-password');
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [authError, setAuthError] = useState('');

  const loadData = async () => {
    const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081';
    if (!token) return;

    try {
      const headers = { Authorization: `Bearer ${token}` };
      const [productResponse, orderResponse] = await Promise.all([
        fetch(`${apiBase}/api/v1/products`, { headers }),
        fetch(`${apiBase}/api/v1/orders`, { headers }),
      ]);

      if (!productResponse.ok) {
        throw new Error(`Products request failed: ${productResponse.status}`);
      }
      if (!orderResponse.ok) {
        throw new Error(`Orders request failed: ${orderResponse.status}`);
      }

      const productData = await productResponse.json();
      const orderData = await orderResponse.json();

      setProducts(productData);
      setOrders(orderData);
      setMode('real');
    } catch {
      setProducts([]);
      setOrders([]);
      setMode('demo');
    }
  };

  useEffect(() => {
    if (token) void loadData();
  }, [token]);

  const handleLogin = async (event: FormEvent) => {
    event.preventDefault();
    const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081';
    setIsLoggingIn(true);
    setAuthError('');
    try {
      const response = await fetch(`${apiBase}/api/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });
      if (!response.ok) throw new Error(response.status === 401 ? 'Invalid email or password.' : 'Login failed.');
      const result = await response.json();
      localStorage.setItem('orderflow_token', result.token);
      setToken(result.token);
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : 'Login failed.');
    } finally {
      setIsLoggingIn(false);
    }
  };

  const handlePlaceOrder = async () => {
    if (!products.length) {
      setStatusMessage('No products are currently available to place an order.');
      return;
    }

    const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081';
    const selectedProduct = products[0];

    setIsSubmitting(true);
    setStatusMessage('');

    try {
      const response = await fetch(`${apiBase}/api/v1/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
          'Idempotency-Key': crypto.randomUUID(),
        },
        body: JSON.stringify({
          items: [{ productId: selectedProduct.id, quantity: 1 }],
        }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Order request failed: ${response.status}`);
      }

      const createdOrder = await response.json();
      setStatusMessage(`Order ${createdOrder.id} created successfully.`);
      await loadData();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to create order.';
      setStatusMessage(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!token) {
    return (
      <main className="auth-shell">
        <form className="auth-panel" onSubmit={handleLogin}>
          <div className="brand">OrderFlow</div>
          <h1>Sign in to commerce operations</h1>
          <label>Email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
          <label>Password<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required /></label>
          {authError && <p className="status-banner">{authError}</p>}
          <button className="action-btn" type="submit" disabled={isLoggingIn}>
            {isLoggingIn ? 'Signing in...' : 'Sign in'}
          </button>
          <p className="auth-hint">Local demo account: admin@acme.test / demo-password</p>
        </form>
      </main>
    );
  }

  const throughputData = useMemo(() => {
    const countsByStatus = orders.reduce<Record<string, number>>((acc, order) => {
      const key = order.status || 'UNKNOWN';
      acc[key] = (acc[key] ?? 0) + 1;
      return acc;
    }, {});

    return Object.entries(countsByStatus).map(([status, count]) => ({
      name: status.replace(/_/g, ' '),
      orders: count,
    }));
  }, [orders]);

  const stats = useMemo(() => {
    const totalOrders = orders.length;
    const successfulOrders = orders.filter((order) => ['CONFIRMED', 'INVENTORY_RESERVED'].includes(order.status)).length;
    const failedOrders = orders.filter((order) => ['CANCELLED', 'PAYMENT_PENDING'].includes(order.status)).length;
    const inventoryReservations = orders.filter((order) => order.status === 'INVENTORY_RESERVED').length;
    const paymentFailures = orders.filter((order) => order.status === 'PAYMENT_PENDING').length;
    const totalRevenue = orders.reduce((sum, order) => sum + Number(order.totalAmount ?? 0), 0);
    const avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;

    return {
      ordersToday: totalOrders,
      ordersPerMinute: Number((totalOrders / 60).toFixed(1)),
      successfulOrders,
      failedOrders,
      inventoryReservations,
      paymentFailures,
      averageOrderValue: Number(avgOrderValue.toFixed(2)),
      totalRevenue: Number(totalRevenue.toFixed(2)),
      productCount: products.length,
    };
  }, [orders, products.length]);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">OrderFlow</div>
        <nav>
          <a href="#">Dashboard</a>
          <a href="#">Products</a>
          <a href="#">Inventory</a>
          <a href="#">Orders</a>
          <a href="#">Events</a>
          <a href="#">System Health</a>
          <a href="#">Failure Lab</a>
          <a href="#">Load Tests</a>
          <a href="#">Architecture</a>
        </nav>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div>
            <h1>Commerce operations dashboard</h1>
            <p className={mode === 'demo' ? 'demo-pill demo' : 'demo-pill real'}>
              {mode === 'demo' ? 'DEMO DATA' : 'REAL DATA'}
            </p>
          </div>
          <button className="action-btn" onClick={handlePlaceOrder} disabled={isSubmitting}>
            {isSubmitting ? 'Creating order...' : 'Create order'}
          </button>
        </header>

        {statusMessage && (
          <div className="status-banner" style={{ marginBottom: '1rem', color: statusMessage.includes('success') ? '#34d399' : '#fbbf24' }}>
            {statusMessage}
          </div>
        )}

        <section className="metrics-grid">
          {Object.entries(stats).map(([label, value]) => (
            <div key={label} className="metric-card">
              <span>{label}</span>
              <strong>{typeof value === 'number' && !Number.isInteger(value) ? value.toFixed(2) : value}</strong>
            </div>
          ))}
        </section>

        <section className="panel-grid">
          <div className="panel">
            <h2>Order status volume</h2>
            <div className="chart-box">
              <ResponsiveContainer width="100%" height={220}>
                <AreaChart data={throughputData.length > 0 ? throughputData : [{ name: 'No data', orders: 0 }]}>
                  <defs>
                    <linearGradient id="ordersFill" x1="0" x2="0" y1="0" y2="1">
                      <stop offset="5%" stopColor="#62d0ff" stopOpacity={0.8} />
                      <stop offset="95%" stopColor="#62d0ff" stopOpacity={0.08} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Area type="monotone" dataKey="orders" stroke="#62d0ff" fill="url(#ordersFill)" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="panel">
            <h2>Recent products</h2>
            <ul className="list">
              {products.length > 0 ? products.slice(0, 5).map((product) => (
                <li key={product.id}><span>{product.name}</span><strong>${Number(product.price).toFixed(2)}</strong></li>
              )) : <li><span>No product data available</span></li>}
            </ul>
          </div>
        </section>

        <section className="panel-grid lower">
          <div className="panel">
            <h2>Order totals</h2>
            <BarChart width={500} height={220} data={orders.slice(0, 6).map((order) => ({ name: order.id.slice(0, 8), value: Number(order.totalAmount ?? 0) }))}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="value" fill="#8b5cf6" />
            </BarChart>
          </div>

          <div className="panel">
            <h2>Recent orders</h2>
            <ul className="list orders">
              {orders.length > 0 ? orders.slice(0, 5).map((order) => (
                <li key={order.id}><span>{order.id}</span><strong>{order.status}</strong></li>
              )) : <li><span>No order data available</span></li>}
            </ul>
          </div>
        </section>
      </main>
    </div>
  );
}
