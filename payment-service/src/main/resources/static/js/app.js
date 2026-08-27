document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('paymentForm');
    const payButton = document.getElementById('payButton');
    const btnText = document.querySelector('.btn-text');
    const loader = document.querySelector('.loader');
    
    const toast = document.getElementById('errorToast');
    const closeToast = document.getElementById('closeToast');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // UI Loading State
        payButton.classList.add('loading');
        loader.classList.remove('hidden');
        payButton.disabled = true;
        
        // Hide any existing toast
        toast.classList.remove('show');

        const requestBody = {
            merchantCode: document.getElementById('merchantCode').value,
            amount: document.getElementById('amount').value
        };

        try {
            // Attempt to call our backend API which contains the deliberate NullPointerException
            const response = await fetch('/api/payments', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                // If we get an HTTP error status (like our deliberate 500)
                throw new Error(`HTTP Error: ${response.status}`);
            }

            // In a correct system, we would handle success here
            // const data = await response.json();
            // showSuccess("Payment processed successfully!");

        } catch (error) {
            // The backend throws HTTP 500, dropping us here
            setTimeout(() => {
                showErrorToast();
            }, 600); // Artificial delay to let the user see the loading state briefly
        } finally {
            // Restore button state
            setTimeout(() => {
                payButton.classList.remove('loading');
                loader.classList.add('hidden');
                payButton.disabled = false;
            }, 600);
        }
    });

    closeToast.addEventListener('click', () => {
        toast.classList.remove('show');
    });

    function showErrorToast() {
        toast.classList.add('show');
        
        // Auto hide after 8 seconds
        setTimeout(() => {
            if (toast.classList.contains('show')) {
                toast.classList.remove('show');
            }
        }, 8000);
    }
});
