
window.onload = function () {
    var lista = document.getElementById('inputType');
    var input = document.getElementById('inputSearch');
    lista.addEventListener('change', () => {
        if (lista.value === "0") {
            input.setAttribute("type", "text");
            input.setAttribute("placeholder", "Especialidad");
            input.removeAttribute("step");
            input.removeAttribute("min");
        } else {
            input.setAttribute("type", "number");
            input.setAttribute("placeholder", "Precio Q.");
            input.setAttribute("step", "any");
            input.setAttribute("min", "0");
        }
    });
};

setSpecialty = function (id, degree, price) {
    $('#specialtyId').val(id);
    $('#degree').val(degree);
    $('#price').val(price);
};

setExam = function (id, name, order, desc, price, report) {
    var btnSubmit = document.getElementById('btn-submit');
    btnSubmit.value = "editExam";
    btnSubmit.textContent = "Editar";
    
    $('#examId').val(id);
    $('#name').val(name);
    $('#order').val(order);
    $('#report').val(report);
    $('#price').val(price);
    $('#description').val(desc);
};

newExam = function () {
    var btnSubmit = document.getElementById('btn-submit');
    btnSubmit.value = "newExam";
    btnSubmit.textContent = "Agregar";

    $('#examId').val("0");
};