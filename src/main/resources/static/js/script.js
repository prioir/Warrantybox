// WarrantyBox - small client-side helpers (no frameworks)

document.addEventListener('DOMContentLoaded', function () {
    // Confirm before deleting a product.
    document.querySelectorAll('.js-confirm-delete').forEach(function (button) {
        button.addEventListener('click', function (event) {
            var confirmed = window.confirm('Delete this product? This action cannot be undone.');
            if (!confirmed) {
                event.preventDefault();
            }
        });
    });

    // Show the chosen invoice file name next to the file input, if present.
    var fileInput = document.getElementById('invoiceFile');
    if (fileInput) {
        fileInput.addEventListener('change', function () {
            var hint = fileInput.parentElement.querySelector('.field-hint');
            if (hint && fileInput.files && fileInput.files.length > 0) {
                hint.textContent = 'Selected file: ' + fileInput.files[0].name;
            }
        });
    }

    // Auto-dismiss success alerts after a few seconds.
    document.querySelectorAll('.alert-success').forEach(function (alert) {
        setTimeout(function () {
            alert.style.transition = 'opacity 0.4s ease';
            alert.style.opacity = '0';
            setTimeout(function () { alert.remove(); }, 400);
        }, 4000);
    });
});
