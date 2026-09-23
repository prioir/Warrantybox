// =========================================================
// WarrantyBox - Client-side Helpers
// No frameworks
// =========================================================

document.addEventListener('DOMContentLoaded', function () {

    /* -----------------------------------------------------
       Confirm before deleting a product
    ----------------------------------------------------- */

    document
        .querySelectorAll('.js-confirm-delete')
        .forEach(function (button) {

            button.addEventListener('click', function (event) {

                var confirmed = window.confirm(
                    'Delete this product? This action cannot be undone.'
                );

                if (!confirmed) {
                    event.preventDefault();
                }
            });
        });


    /* -----------------------------------------------------
       Show selected invoice file name
    ----------------------------------------------------- */

    var fileInput = document.getElementById('invoiceFile');

    if (fileInput) {

        fileInput.addEventListener('change', function () {

            var hint = fileInput.parentElement
                ? fileInput.parentElement.querySelector('.field-hint')
                : null;

            if (
                hint &&
                fileInput.files &&
                fileInput.files.length > 0
            ) {
                hint.textContent =
                    'Selected file: ' +
                    fileInput.files[0].name;
            }
        });
    }


    /* -----------------------------------------------------
       Auto-dismiss success alerts
    ----------------------------------------------------- */

    document
        .querySelectorAll('.alert-success')
        .forEach(function (alert) {

            setTimeout(function () {

                alert.style.transition =
                    'opacity 0.4s ease, transform 0.4s ease';

                alert.style.opacity = '0';
                alert.style.transform = 'translateY(-5px)';

                setTimeout(function () {
                    alert.remove();
                }, 400);

            }, 4000);
        });

});