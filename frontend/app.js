// CodeGuardian Failure Lab — Frontend Commerce Application
// Dynamic configuration & REST API client

const CONFIG_KEY_GATEWAY = 'cg_gateway_url';
const CONFIG_KEY_CODEGUARDIAN = 'cg_app_url';

// Default URLs
let gatewayUrl = localStorage.getItem(CONFIG_KEY_GATEWAY) || 'http://localhost:8080';
let codeGuardianUrl = localStorage.getItem(CONFIG_KEY_CODEGUARDIAN) || 'http://localhost:5173';

// URL params override
const urlParams = new URLSearchParams(window.location.search);
if (urlParams.has('backend')) {
    gatewayUrl = urlParams.get('backend');
}
if (urlParams.has('codeguardian')) {
    codeGuardianUrl = urlParams.get('codeguardian');
}

// In-Memory State
let products = [];
let cart = [];
let lastFailurePayload = null;

// DOM Elements
const healthDot = document.getElementById('health-dot');
const healthText = document.getElementById('health-text');
const productsGrid = document.getElementById('products-grid');
const searchInput = document.getElementById('product-search-input');
const clearSearchBtn = document.getElementById('clear-search-btn');
const ordersTbody = document.getElementById('orders-tbody');
const cartDrawer = document.getElementById('cart-drawer');
const cartItemsContainer = document.getElementById('cart-items-container');
const cartCountBadge = document.getElementById('cart-count-badge');
const cartSubtotal = document.getElementById('cart-subtotal');
const cartTotal = document.getElementById('cart-total');
const cartToggleBtn = document.getElementById('cart-toggle-btn');
const cartCloseBtn = document.getElementById('cart-close-btn');

// Config Elements
const configModal = document.getElementById('config-modal');
const configBtn = document.getElementById('config-btn');
const configCloseBtn = document.getElementById('config-close-btn');
const gatewayUrlInput = document.getElementById('gateway-url-input');
const codeguardianUrlInput = document.getElementById('codeguardian-url-input');

// Failure Modal Elements
const failureModal = document.getElementById('failure-modal');
const failureHttpStatus = document.getElementById('failure-http-status');
const failureErrorCode = document.getElementById('failure-error-code');
const failureReqId = document.getElementById('failure-req-id');
const failureMessage = document.getElementById('failure-message');
const techDetailsContent = document.getElementById('tech-details-content');
const techAccordionIcon = document.getElementById('tech-accordion-icon');

// Success Modal Elements
const successModal = document.getElementById('success-modal');
const successOrderNum = document.getElementById('success-order-num');
const successCorrId = document.getElementById('success-corr-id');
const toastEl = document.getElementById('toast');

// Helper: Generate unique Request ID
function generateRequestId() {
    return 'req-' + Math.random().toString(36).substring(2, 10);
}

// Toast helper
function showToast(message) {
    if (!toastEl) return;
    toastEl.textContent = message;
    toastEl.classList.remove('hidden');
    setTimeout(() => {
        toastEl.classList.add('hidden');
    }, 3000);
}

// API Helper with correlation header
async function apiFetch(endpoint, options = {}) {
    const requestId = generateRequestId();
    const headers = {
        'Content-Type': 'application/json',
        'X-Request-ID': requestId,
        ...(options.headers || {})
    };

    const url = `${gatewayUrl.replace(/\/$/, '')}${endpoint}`;
    
    // Update live correlation UI
    const corrBadge = document.getElementById('live-corr-id');
    if (corrBadge) {
        corrBadge.textContent = `CorrID: ${requestId}`;
    }

    try {
        const response = await fetch(url, {
            ...options,
            headers
        });

        const contentType = response.headers.get('content-type');
        let data = null;
        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
        } else {
            const text = await response.text();
            try {
                data = JSON.parse(text);
            } catch (e) {
                data = { raw: text };
            }
        }

        return {
            ok: response.ok,
            status: response.status,
            requestId: response.headers.get('X-Request-ID') || requestId,
            data
        };
    } catch (err) {
        return {
            ok: false,
            status: 0,
            requestId,
            error: err.message
        };
    }
}

// Health Check
async function checkHealth() {
    const res = await apiFetch('/health');
    if (res.ok && res.data && res.data.status === 'UP') {
        healthDot.className = 'health-dot online';
        healthText.textContent = 'Backend Online';
    } else {
        healthDot.className = 'health-dot offline';
        healthText.textContent = res.status === 0 ? 'Backend Unreachable' : `Status: ${res.status}`;
    }
}

// Load Products from Backend
async function loadProducts(searchQuery = '') {
    productsGrid.innerHTML = '<div class="loading-spinner">Loading products from backend...</div>';
    
    const endpoint = searchQuery ? `/products/search?q=${encodeURIComponent(searchQuery)}` : '/products';
    const res = await apiFetch(endpoint);

    if (res.ok && Array.isArray(res.data)) {
        products = res.data;
        renderProducts(products);
    } else {
        // Fallback default demo items if backend hasn't booted yet
        productsGrid.innerHTML = `
            <div class="empty-state" style="grid-column: 1/-1; text-align: center; padding: 2rem;">
                <p style="color: var(--accent-danger); margin-bottom: 0.5rem;">Failed to load products from Gateway (${gatewayUrl})</p>
                <p style="color: var(--text-muted); font-size: 0.85rem;">Ensure gateway (:8080) and order-service (:8081) are running.</p>
                <button class="btn btn-secondary btn-sm" style="margin-top: 1rem;" onclick="loadProducts()">Retry</button>
            </div>
        `;
    }
}

// Render Products Grid
function renderProducts(items) {
    if (!items || items.length === 0) {
        productsGrid.innerHTML = '<div class="empty-state" style="grid-column: 1/-1; text-align: center; padding: 2rem; color: var(--text-muted);">No products match your search.</div>';
        return;
    }

    productsGrid.innerHTML = items.map(p => `
        <div class="product-card glass-panel">
            <img src="${p.imageUrl || 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=600&auto=format&fit=crop&q=80'}" alt="${p.name}" class="product-img">
            <div class="product-body">
                <div>
                    <div class="product-category">${p.category || 'Security'}</div>
                    <h3 class="product-name">${p.name}</h3>
                    <p class="product-desc">${p.description}</p>
                </div>
                <div class="product-foot">
                    <span class="product-price">$${Number(p.price).toFixed(2)}</span>
                    <button class="btn btn-primary btn-sm" onclick="addToCart(${p.id})">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                        Add
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

// Load Orders History from Backend
async function loadOrders() {
    ordersTbody.innerHTML = '<tr><td colspan="6" class="text-center">Loading orders from backend...</td></tr>';
    const res = await apiFetch('/orders');
    
    if (res.ok && Array.isArray(res.data)) {
        if (res.data.length === 0) {
            ordersTbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No orders in database yet.</td></tr>';
            return;
        }

        ordersTbody.innerHTML = res.data.map(o => `
            <tr>
                <td><strong>${o.orderNumber || 'ORD-' + o.id}</strong></td>
                <td>${o.userId}</td>
                <td><code>${o.merchantCode || 'N/A'}</code></td>
                <td>$${Number(o.totalAmount || 0).toFixed(2)}</td>
                <td>
                    <span class="badge ${o.status === 'CONFIRMED' ? 'badge-success' : 'badge-danger'}">
                        ${o.status}
                    </span>
                </td>
                <td style="color: var(--text-muted); font-size: 0.8rem;">
                    ${o.createdAt ? new Date(o.createdAt).toLocaleTimeString() : 'Recent'}
                </td>
            </tr>
        `).join('');
    } else {
        ordersTbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Failed to fetch orders from Gateway</td></tr>';
    }
}

// Cart Operations
function addToCart(productId) {
    const product = products.find(p => p.id === productId);
    if (!product) return;

    const existing = cart.find(item => item.id === productId);
    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({ ...product, quantity: 1 });
    }

    updateCartUI();
    showToast(`Added "${product.name}" to cart`);
}

function removeFromCart(productId) {
    cart = cart.filter(item => item.id !== productId);
    updateCartUI();
}

function updateCartUI() {
    const totalCount = cart.reduce((sum, item) => sum + item.quantity, 0);
    cartCountBadge.textContent = totalCount;

    if (cart.length === 0) {
        cartItemsContainer.innerHTML = '<div class="empty-cart">Your cart is empty. Add products from the catalog above!</div>';
        cartSubtotal.textContent = '$0.00';
        cartTotal.textContent = '$0.00';
        return;
    }

    let subtotal = 0;
    cartItemsContainer.innerHTML = cart.map(item => {
        const itemTotal = item.price * item.quantity;
        subtotal += itemTotal;
        return `
            <div class="cart-item">
                <div>
                    <strong style="font-size: 0.9rem;">${item.name}</strong>
                    <div style="color: var(--text-muted); font-size: 0.8rem;">$${item.price.toFixed(2)} × ${item.quantity}</div>
                </div>
                <div style="display: flex; align-items: center; gap: 0.5rem;">
                    <strong style="color: #fff;">$${itemTotal.toFixed(2)}</strong>
                    <button class="icon-btn" style="width: 28px; height: 28px; font-size: 0.75rem;" onclick="removeFromCart(${item.id})">&times;</button>
                </div>
            </div>
        `;
    }).join('');

    cartSubtotal.textContent = `$${subtotal.toFixed(2)}`;
    cartTotal.textContent = `$${subtotal.toFixed(2)}`;
}

// Scenario Trigger
async function runScenario(scenarioId) {
    let payload = {};
    if (scenarioId === '5001') {
        payload = {
            userId: 101,
            orderId: 5001,
            amount: 499.0,
            merchantCode: 'MCH-UNKNOWN'
        };
    } else if (scenarioId === '5002') {
        payload = {
            userId: 101,
            orderId: 5002,
            amount: 149.0,
            merchantCode: 'MCH-5002'
        };
    } else if (scenarioId === '5003') {
        payload = {
            userId: 102,
            orderId: 5003,
            amount: 299.0,
            merchantCode: 'MCH-5003'
        };
    }

    showToast(`Executing Scenario ${scenarioId}...`);
    await executeCheckout(payload);
}

// Checkout Cart
async function checkoutCart(merchantCode) {
    if (cart.length === 0) {
        showToast('Cart is empty!');
        return;
    }

    const total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const orderId = (merchantCode === 'MCH-UNKNOWN') ? 5001 : 5002;

    const payload = {
        userId: 101,
        orderId: orderId,
        amount: total,
        merchantCode: merchantCode,
        productIds: cart.map(i => i.id)
    };

    cartDrawer.classList.add('hidden');
    await executeCheckout(payload);
}

// Execute Checkout API Call
async function executeCheckout(payload) {
    const res = await apiFetch('/checkout', {
        method: 'POST',
        body: JSON.stringify(payload)
    });

    // Refresh orders list
    loadOrders();

    if (res.ok) {
        // Success 200 OK
        openSuccessModal(payload.orderId, res.requestId);
    } else {
        // Failure (e.g. 500 with NULL_OBJECT_ACCESS)
        openFailureModal(res, payload);
    }
}

// Open Failure Modal
function openFailureModal(res, requestPayload) {
    const data = res.data || {};
    lastFailurePayload = {
        timestamp: data.timestamp || new Date().toISOString(),
        requestId: res.requestId || data.requestId || 'req-unknown',
        endpoint: '/checkout',
        httpStatus: res.status || data.status || 500,
        errorCode: data.errorCode || 'NULL_OBJECT_ACCESS',
        message: data.message || 'Payment processing failed because merchant data was unavailable',
        service: data.service || 'payment-service',
        exception: data.exception || 'NullPointerException',
        source: data.source || {
            file: 'PaymentService.java',
            line: 24
        },
        request: requestPayload
    };

    failureHttpStatus.textContent = `HTTP ${lastFailurePayload.httpStatus}`;
    failureErrorCode.textContent = lastFailurePayload.errorCode;
    failureReqId.textContent = `Request ID: ${lastFailurePayload.requestId}`;
    failureMessage.textContent = lastFailurePayload.message;

    // Populate Technical Details
    document.getElementById('tech-timestamp').textContent = lastFailurePayload.timestamp;
    document.getElementById('tech-request-id').textContent = lastFailurePayload.requestId;
    document.getElementById('tech-endpoint').textContent = lastFailurePayload.endpoint;
    document.getElementById('tech-status').textContent = lastFailurePayload.httpStatus;
    document.getElementById('tech-code').textContent = lastFailurePayload.errorCode;
    document.getElementById('tech-service').textContent = lastFailurePayload.service;
    document.getElementById('tech-exception').textContent = lastFailurePayload.exception;
    document.getElementById('tech-file').textContent = lastFailurePayload.source.file || 'PaymentService.java';
    document.getElementById('tech-line').textContent = lastFailurePayload.source.line || '24';

    // Hide technical accordion by default
    techDetailsContent.classList.add('hidden');
    techAccordionIcon.style.transform = 'rotate(0deg)';

    failureModal.classList.remove('hidden');
}

function toggleTechDetails() {
    const isHidden = techDetailsContent.classList.toggle('hidden');
    techAccordionIcon.style.transform = isHidden ? 'rotate(0deg)' : 'rotate(180deg)';
}

function copyFailureDetails() {
    if (!lastFailurePayload) return;
    navigator.clipboard.writeText(JSON.stringify(lastFailurePayload, null, 2))
        .then(() => showToast('Failure details copied to clipboard!'))
        .catch(() => showToast('Failed to copy to clipboard.'));
}

function openCodeGuardianInvestigation() {
    if (!lastFailurePayload) return;
    
    // Construct incident link with query parameters
    const params = new URLSearchParams({
        repo: 'https://github.com/sunilkumarb2007/JavaAPICheck',
        requestId: lastFailurePayload.requestId,
        errorCode: lastFailurePayload.errorCode,
        service: lastFailurePayload.service,
        file: lastFailurePayload.source.file,
        line: lastFailurePayload.source.line
    });

    const targetUrl = `${codeGuardianUrl.replace(/\/$/, '')}?${params.toString()}`;
    window.open(targetUrl, '_blank');
}

function closeFailureModal() {
    failureModal.classList.add('hidden');
}

// Success Modal
function openSuccessModal(orderId, requestId) {
    successOrderNum.textContent = `Order #${orderId || 5002} has been verified and confirmed.`;
    successCorrId.textContent = requestId;
    successModal.classList.remove('hidden');
}

function closeSuccessModal() {
    successModal.classList.add('hidden');
}

// Config Modal Operations
function openConfigModal() {
    gatewayUrlInput.value = gatewayUrl;
    codeguardianUrlInput.value = codeGuardianUrl;
    configModal.classList.remove('hidden');
}

function closeConfigModal() {
    configModal.classList.add('hidden');
}

function saveConfig() {
    gatewayUrl = gatewayUrlInput.value.trim() || 'http://localhost:8080';
    codeGuardianUrl = codeguardianUrlInput.value.trim() || 'http://localhost:5173';
    localStorage.setItem(CONFIG_KEY_GATEWAY, gatewayUrl);
    localStorage.setItem(CONFIG_KEY_CODEGUARDIAN, codeGuardianUrl);
    closeConfigModal();
    showToast('Saved backend configuration');
    checkHealth();
    loadProducts();
    loadOrders();
}

function resetConfig() {
    gatewayUrl = 'http://localhost:8080';
    codeGuardianUrl = 'http://localhost:5173';
    localStorage.removeItem(CONFIG_KEY_GATEWAY);
    localStorage.removeItem(CONFIG_KEY_CODEGUARDIAN);
    gatewayUrlInput.value = gatewayUrl;
    codeguardianUrlInput.value = codeGuardianUrl;
}

// Event Listeners
document.addEventListener('DOMContentLoaded', () => {
    // Initial health & data fetch
    checkHealth();
    loadProducts();
    loadOrders();

    // Periodic health check every 10 seconds
    setInterval(checkHealth, 10000);

    // Search input listener
    let debounceTimer;
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            const query = e.target.value;
            if (clearSearchBtn) {
                clearSearchBtn.classList.toggle('hidden', query.length === 0);
            }
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                loadProducts(query);
            }, 300);
        });
    }

    if (clearSearchBtn) {
        clearSearchBtn.addEventListener('click', () => {
            searchInput.value = '';
            clearSearchBtn.classList.add('hidden');
            loadProducts('');
        });
    }

    // Cart Drawer Toggle
    if (cartToggleBtn) cartToggleBtn.addEventListener('click', () => cartDrawer.classList.remove('hidden'));
    if (cartCloseBtn) cartCloseBtn.addEventListener('click', () => cartDrawer.classList.add('hidden'));

    // Config Modal Toggle
    if (configBtn) configBtn.addEventListener('click', openConfigModal);
    if (configCloseBtn) configCloseBtn.addEventListener('click', closeConfigModal);
});
